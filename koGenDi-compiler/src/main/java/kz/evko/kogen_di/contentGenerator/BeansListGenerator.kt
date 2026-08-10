package kz.evko.kogen_di.contentGenerator

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import kz.evko.kogen_di.annotations.KoGenBean

/**
 * Builds `KoGenBeansImpl.kt` (one enum entry per `@KoGenBean` function, via [generateBeansList])
 * and `KoGenBeansFactoryImpl.kt` (that function's return type mapped to its entry, via
 * [generateBeansFactory]).
 */
class BeansListGenerator(
    private val logger: KSPLogger,
    private val packageName: String,
) {
    private val koGenBeansInterface = ClassName("kz.evko.kogen_di.injector", "KoGenBeans")
    private val koGenBeansFactoryClass = ClassName("kz.evko.kogen_di.injector", "KoGenBeansFactory")

    /**
     * The `KoGenBeansImpl` enum implementing `KoGenBeans` - one entry per bean function, each
     * with its function's `@KoGenBean(singleton = ...)` flag baked in, and a shared
     * `getComponentObject()` override that calls the matching function (resolving its own
     * parameters via `inject()`).
     */
    fun generateBeansList(beans: List<KSFunctionDeclaration>): FileSpec {
        val enumBuilder = TypeSpec.enumBuilder("KoGenBeansImpl")
            .addSuperinterface(koGenBeansInterface)
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

        beans.forEach { bean ->
            val returnDeclaration = bean.returnType?.resolve()?.declaration ?: return@forEach
            val isSingleton = bean.findSingletonParam(KoGenBean::class)
            enumBuilder.addEnumConstant(
                returnDeclaration.createComponentNames(),
                TypeSpec.anonymousClassBuilder()
                    .addSuperclassConstructorParameter("singleton = %L", isSingleton)
                    .build()
            )
        }

        val body = CodeBlock.builder()
        if (beans.isEmpty()) {
            body.addStatement("return %T()", ANY)
        } else {
            body.beginControlFlow("return when (this)")
            beans.forEach { bean ->
                val returnDeclaration = bean.returnType?.resolve()?.declaration ?: return@forEach
                val functionMember = MemberName(
                    bean.packageName.asString(),
                    bean.simpleName.asString(),
                )
                body.beginControlFlow("%N ->", returnDeclaration.createComponentNames())
                if (bean.parameters.isEmpty()) {
                    body.addStatement("%M()", functionMember)
                } else {
                    body.add("%M(\n", functionMember)
                    body.indent()
                    bean.parameters.forEach { parameter ->
                        body.addStatement("%N = inject(),", parameter.name?.asString().orEmpty())
                    }
                    body.unindent()
                    body.addStatement(")")
                }
                body.endControlFlow()
            }
            body.endControlFlow()
        }

        val getComponentObjectFun = FunSpec.builder("getComponentObject")
            .addModifiers(KModifier.OVERRIDE)
            .returns(ANY)
            .addCode(body.build())
            .build()

        enumBuilder.addFunction(getComponentObjectFun)

        return FileSpec.builder(packageName, "KoGenBeansImpl")
            .addType(enumBuilder.build())
            .build()
    }

    /** The `KoGenBeansFactoryImpl` subclass of `KoGenBeansFactory` - maps each bean function's return type to its `KoGenBeansImpl` entry. */
    fun generateBeansFactory(beans: List<KSFunctionDeclaration>): FileSpec {
        val koGenBeansImplClass = ClassName(packageName, "KoGenBeansImpl")
        val classOfStar = ClassName("java.lang", "Class").parameterizedBy(STAR)
        val mapType = ClassName("kotlin.collections", "Map")
            .parameterizedBy(classOfStar, koGenBeansInterface)

        val mapBody = CodeBlock.builder().add("mapOf(\n").indent()
        beans.forEach { bean ->
            val returnDeclaration = bean.returnType?.resolve()?.declaration ?: return@forEach
            val returnTypeClassName = ClassName(
                returnDeclaration.packageName.asString(),
                returnDeclaration.simpleName.asString(),
            )
            mapBody.addStatement(
                "%T::class.java to %T.%N,",
                returnTypeClassName,
                koGenBeansImplClass,
                returnDeclaration.createComponentNames(),
            )
        }
        mapBody.unindent().add(")")

        val createBeansListFun = FunSpec.builder("createBeansList")
            .addModifiers(KModifier.OVERRIDE)
            .returns(mapType)
            .addStatement("return %L", mapBody.build())
            .build()

        val classSpec = TypeSpec.classBuilder("KoGenBeansFactoryImpl")
            .superclass(koGenBeansFactoryClass)
            .addFunction(createBeansListFun)
            .build()

        return FileSpec.builder(packageName, "KoGenBeansFactoryImpl")
            .addType(classSpec)
            .build()
    }
}
