package kz.evko.kogen_di.contentGenerator

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import kz.evko.kogen_di.annotations.KoGenComponent
import kotlin.reflect.KClass

/**
 * Builds `KoGenComponentsImpl.kt` (one enum entry per `@KoGenComponent` class, via
 * [generateComponentList]) and `KoGenComponentsFactoryImpl.kt` (that class's own type *and* every
 * supertype it satisfies mapped to its entry, via [createComponentFactory]).
 */
class ComponentListGenerator(
    private val logger: KSPLogger,
    private val packageName: String,
) {
    private val koGenComponentsInterface = ClassName("kz.evko.kogen_di.injector", "KoGenComponents")
    private val koGenComponentsFactoryClass = ClassName("kz.evko.kogen_di.injector", "KoGenComponentsFactory")
    private val classOfStar = ClassName("java.lang", "Class").parameterizedBy(STAR)

    /**
     * The `KoGenComponentsImpl` enum implementing `KoGenComponents` - one entry per component
     * class, each with its class's `@KoGenComponent(singleton = ...)` flag baked in, and a shared
     * `getComponentObject()` override that constructs the matching class (resolving its
     * constructor parameters via `inject()`).
     */
    fun generateComponentList(components: List<KSClassDeclaration>): FileSpec {
        val enumBuilder = TypeSpec.enumBuilder("KoGenComponentsImpl")
            .addSuperinterface(koGenComponentsInterface)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("singleton", BOOLEAN)
                    .build()
            )
            .addProperty(
                PropertySpec.builder("singleton", BOOLEAN)
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer("singleton")
                    .build()
            )

        components.forEach { component ->
            val isSingleton = component.findSingletonParam(KoGenComponent::class)
            enumBuilder.addEnumConstant(
                component.createComponentNames(),
                TypeSpec.anonymousClassBuilder()
                    .addSuperclassConstructorParameter("%L", isSingleton)
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

    /** The `KoGenComponentsFactoryImpl` subclass of `KoGenComponentsFactory` - maps each component's own type and every [satisfiableClassNames] supertype to its `KoGenComponentsImpl` entry. */
    fun createComponentFactory(components: List<KSClassDeclaration>): FileSpec {
        val koGenComponentsImplClass = ClassName(packageName, "KoGenComponentsImpl")
        val mapType = ClassName("kotlin.collections", "Map")
            .parameterizedBy(classOfStar, koGenComponentsInterface)

        val mapBody = CodeBlock.builder().add("mapOf(\n").indent()
        components.forEach { component ->
            val enumName = component.createComponentNames()
            component.satisfiableClassNames().forEach { satisfiableClass ->
                mapBody.addStatement(
                    "%T::class.java to %T.%N,",
                    satisfiableClass,
                    koGenComponentsImplClass,
                    enumName,
                )
            }
        }
        mapBody.unindent().add(")")

        val createComponentsMapFun = FunSpec.builder("createComponentsMap")
            .addModifiers(KModifier.OVERRIDE)
            .returns(mapType)
            .addStatement("return %L", mapBody.build())
            .build()

        val classSpec = TypeSpec.classBuilder("KoGenComponentsFactoryImpl")
            .superclass(koGenComponentsFactoryClass)
            .addFunction(createComponentsMapFun)
            .build()

        return FileSpec.builder(packageName, "KoGenComponentsFactoryImpl")
            .addType(classSpec)
            .build()
    }

    // Рекурсивно, а не только прямые supertype - должно совпадать с тем, что считает
    // "удовлетворяемым" DependencyValidator (KoGenProvider.getSuperTypes()), иначе
    // возможна ситуация "валидация прошла, а inject<T>() всё равно не находит компонент".
    private fun KSClassDeclaration.satisfiableClassNames(): List<ClassName> {
        val result = linkedSetOf(ClassName(packageName.asString(), simpleName.asString()))

        fun collect(declaration: KSClassDeclaration) {
            declaration.superTypes.forEach { typeReference ->
                val superDeclaration = typeReference.resolve().declaration as? KSClassDeclaration
                    ?: return@forEach
                val qualifiedName = superDeclaration.qualifiedName?.asString() ?: return@forEach
                if (qualifiedName == "kotlin.Any") return@forEach

                val className = ClassName(
                    superDeclaration.packageName.asString(),
                    superDeclaration.simpleName.asString(),
                )
                if (result.add(className)) {
                    collect(superDeclaration)
                }
            }
        }
        collect(this)

        return result.toList()
    }
}

/** This declaration's `singleton` argument for [annotationClass] (`@KoGenComponent`/`@KoGenBean`), or `false` if unset/absent. */
internal fun KSDeclaration.findSingletonParam(annotationClass: KClass<*>): Boolean {
    val annotation =
        this.annotations.firstOrNull { it.shortName.asString() == annotationClass.simpleName }
    val name = annotation?.arguments?.firstOrNull { it.name?.asString() == "singleton" }
    return name?.value == true
}

/** This declaration's fully-qualified name. */
internal fun KSDeclaration.getName(): String =
    packageName.asString() + "." + simpleName.asString()

/** This declaration's generated enum-entry name - its fully-qualified name with every `.` turned into `_`, since a qualified name isn't valid as an identifier. */
internal fun KSDeclaration.createComponentNames(): String =
    getName().replace(".", "_")
