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
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec

class ViewModelListGenerator(
    private val logger: KSPLogger,
    private val packageName: String,
) {
    private val koGenViewModelsInterface =
        ClassName("kz.evko.kogen_di.viewModel", "KoGenViewModels")
    private val koGenViewModelScopeClass =
        ClassName("kz.evko.kogen_di.viewModel", "KoGenViewModelScope")
    private val classOfStar = ClassName("java.lang", "Class").parameterizedBy(STAR)

    fun generateViewModelList(viewModels: List<KSClassDeclaration>): FileSpec {
        val enumBuilder = TypeSpec.enumBuilder("KoGenViewModelsImpl")
            .addSuperinterface(koGenViewModelsInterface)

        viewModels.forEach { viewModel ->
            enumBuilder.addEnumConstant(viewModel.createComponentNames())
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

    fun generateViewModelFactory(viewModels: List<KSClassDeclaration>): FileSpec {
        val koGenViewModelsImplClass = ClassName(packageName, "KoGenViewModelsImpl")
        val mapType = ClassName("kotlin.collections", "Map")
            .parameterizedBy(classOfStar, koGenViewModelsInterface)

        val mapBody = CodeBlock.builder().add("mapOf(\n").indent()
        viewModels.forEach { viewModel ->
            val viewModelClass = ClassName(
                viewModel.packageName.asString(),
                viewModel.simpleName.asString(),
            )
            mapBody.addStatement(
                "%T::class.java to %T.%N,",
                viewModelClass,
                koGenViewModelsImplClass,
                viewModel.createComponentNames(),
            )
        }
        mapBody.unindent().add(")")

        val createViewModelsMapFun = FunSpec.builder("createViewModelsMap")
            .addModifiers(KModifier.OVERRIDE)
            .returns(mapType)
            .addStatement("return %L", mapBody.build())
            .build()

        val classSpec = TypeSpec.classBuilder("KoGenViewModelScopeImpl")
            .superclass(koGenViewModelScopeClass)
            .addFunction(createViewModelsMapFun)
            .build()

        return FileSpec.builder(packageName, "KoGenViewModelScopeImpl")
            .addType(classSpec)
            .build()
    }
}
