package kz.evko.kogen_di.contentGenerator

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec

class ViewModelListGenerator(
    private val logger: KSPLogger,
    private val packageName: String,
) {
    private val koGenViewModelsInterface =
        ClassName("kz.evko.kogen_di.viewModel", "KoGenViewModels")
    private val koGenViewModelScopeClass =
        ClassName("kz.evko.kogen_di.viewModel", "KoGenViewModelScope")

    fun generateViewModelList(viewModels: List<KSClassDeclaration>): FileSpec {
        val enumBuilder = TypeSpec.enumBuilder("KoGenViewModelsImpl")
            .addSuperinterface(koGenViewModelsInterface)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("fullName", STRING)
                    .build()
            )
            .addProperty(
                PropertySpec.builder("fullName", STRING)
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer("fullName")
                    .build()
            )

        viewModels.forEach { viewModel ->
            enumBuilder.addEnumConstant(
                viewModel.createComponentNames(),
                TypeSpec.anonymousClassBuilder()
                    .addSuperclassConstructorParameter("%S", viewModel.getName())
                    .build()
            )
        }

        val body = CodeBlock.builder()
        if (viewModels.isEmpty()) {
            body.addStatement("return %T()", ANY)
        } else {
            body.beginControlFlow("return when (this)")
            viewModels.forEach { viewModel ->
                val viewModelClass = ClassName(
                    viewModel.packageName.asString(),
                    viewModel.simpleName.asString(),
                )
                val parameters = viewModel.primaryConstructor?.parameters.orEmpty()
                if (parameters.isEmpty()) {
                    body.addStatement(
                        "%N -> %T()",
                        viewModel.createComponentNames(),
                        viewModelClass
                    )
                } else {
                    body.add("%N -> %T(\n", viewModel.createComponentNames(), viewModelClass)
                    body.indent()
                    parameters.forEach { param ->
                        body.addStatement("%N = inject(),", param.name?.asString().orEmpty())
                    }
                    body.unindent()
                    body.addStatement(")")
                }
            }
            body.endControlFlow()
        }

        val getComponentObjectFun = FunSpec.builder("getComponentObject")
            .addModifiers(KModifier.OVERRIDE)
            .returns(ANY)
            .addCode(body.build())
            .build()

        enumBuilder.addFunction(getComponentObjectFun)

        return FileSpec.builder(packageName, "KoGenViewModelsImpl")
            .addType(enumBuilder.build())
            .build()
    }

    fun generateViewModelFactory(): FileSpec {
        val koGenViewModelsImplClass = ClassName(packageName, "KoGenViewModelsImpl")
        val listOfViewModels = ClassName("kotlin.collections", "List")
            .parameterizedBy(koGenViewModelsInterface)

        val componentsListFun = FunSpec.builder("componentsList")
            .addModifiers(KModifier.OVERRIDE)
            .returns(listOfViewModels)
            .addStatement("return %T.entries", koGenViewModelsImplClass)
            .build()

        val classSpec = TypeSpec.classBuilder("KoGenViewModelScopeImpl")
            .superclass(koGenViewModelScopeClass)
            .addFunction(componentsListFun)
            .build()

        return FileSpec.builder(packageName, "KoGenViewModelScopeImpl")
            .addType(classSpec)
            .build()
    }
}
