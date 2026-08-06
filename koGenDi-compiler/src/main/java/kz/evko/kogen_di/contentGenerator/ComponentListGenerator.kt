package kz.evko.kogen_di.contentGenerator

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import kz.evko.kogen_di.annotations.KoGenComponent
import kotlin.reflect.KClass

class ComponentListGenerator(
    private val logger: KSPLogger,
    private val packageName: String,
) {
    private val koGenComponentsInterface = ClassName("kz.evko.kogen_di.injector", "KoGenComponents")
    private val koGenComponentsFactoryClass = ClassName("kz.evko.kogen_di.injector", "KoGenComponentsFactory")
    private val stringArrayOut = ARRAY.parameterizedBy(WildcardTypeName.producerOf(STRING))

    fun generateComponentList(components: List<KSClassDeclaration>): FileSpec {
        val enumBuilder = TypeSpec.enumBuilder("KoGenComponentsImpl")
            .addSuperinterface(koGenComponentsInterface)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("singleton", BOOLEAN)
                    .addParameter(
                        ParameterSpec.builder("componentType", STRING)
                            .addModifiers(KModifier.VARARG)
                            .build()
                    )
                    .build()
            )
            .addProperty(
                PropertySpec.builder("singleton", BOOLEAN)
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer("singleton")
                    .build()
            )
            .addProperty(
                PropertySpec.builder("componentType", stringArrayOut)
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer("componentType")
                    .build()
            )

        components.forEach { component ->
            val enumName = component.createComponentNames()
            val isSingleton = component.findSingletonParam(KoGenComponent::class)
            val names = component.satisfiableNames()

            val format = "%L" + names.joinToString("") { ", %S" }
            val args = mutableListOf<Any>(isSingleton).apply { addAll(names) }

            enumBuilder.addEnumConstant(
                enumName,
                TypeSpec.anonymousClassBuilder()
                    .addSuperclassConstructorParameter(format, *args.toTypedArray())
                    .build()
            )
        }

        val body = CodeBlock.builder()
        if (components.isEmpty()) {
            body.addStatement("return %T()", ANY)
        } else {
            body.beginControlFlow("return when (this)")
            components.forEach { component ->
                val enumName = component.createComponentNames()
                val componentClass = ClassName(
                    component.packageName.asString(),
                    component.simpleName.asString(),
                )
                val parameters = component.primaryConstructor?.parameters.orEmpty()
                if (parameters.isEmpty()) {
                    body.addStatement("%N -> %T()", enumName, componentClass)
                } else {
                    body.add("%N -> %T(\n", enumName, componentClass)
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

        return FileSpec.builder(packageName, "KoGenComponentsImpl")
            .addType(enumBuilder.build())
            .build()
    }

    fun createComponentFactory(): FileSpec {
        val koGenComponentsImplClass = ClassName(packageName, "KoGenComponentsImpl")
        val listOfComponents = ClassName("kotlin.collections", "List")
            .parameterizedBy(koGenComponentsInterface)

        val componentsListFun = FunSpec.builder("componentsList")
            .addModifiers(KModifier.OVERRIDE)
            .returns(listOfComponents)
            .addStatement("return %T.entries", koGenComponentsImplClass)
            .build()

        val classSpec = TypeSpec.classBuilder("KoGenComponentsFactoryImpl")
            .superclass(koGenComponentsFactoryClass)
            .addFunction(componentsListFun)
            .build()

        return FileSpec.builder(packageName, "KoGenComponentsFactoryImpl")
            .addType(classSpec)
            .build()
    }

    private fun KSClassDeclaration.satisfiableNames(): List<String> {
        val names = mutableListOf(getName())

        superTypes.forEach {
            val name = it.resolve().declaration.getName()
            if (name != "kotlin.Any") {
                names.add(name)
            }
        }

        return names
    }
}

internal fun KSDeclaration.findSingletonParam(annotationClass: KClass<*>): Boolean {
    val annotation =
        this.annotations.firstOrNull { it.shortName.asString() == annotationClass.simpleName }
    val name = annotation?.arguments?.firstOrNull { it.name?.asString() == "singleton" }
    return name?.value == true
}

internal fun KSDeclaration.getName(): String =
    packageName.asString() + "." + simpleName.asString()

internal fun KSDeclaration.createComponentNames(): String =
    getName().replace(".", "_")
