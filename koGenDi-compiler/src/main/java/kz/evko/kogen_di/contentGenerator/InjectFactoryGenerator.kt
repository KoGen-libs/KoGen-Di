package kz.evko.kogen_di.contentGenerator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeVariableName

/**
 * Builds `KoGenInjectors.kt` - the `inject()`/`setApplicationContext()` entry points every
 * consumer gets, plus (when the matching KSP option is on) a Compose `koGenViewModel()` and a
 * `Fragment`/`ComponentActivity` `koGenViewModel()` delegate property. Written once per KSP run,
 * regardless of whether anything is annotated - see [generateInjectors].
 *
 * Every entry point here is a one-line delegate to the real implementation living in the `koGenDi`
 * runtime library (`kz.evko.kogen_di.injector`/`kz.evko.kogen_di.viewModel` - see
 * `KoGenInjection.kt`/`KoGenViewModelInjection.kt` there). The only thing that's actually generated
 * per module is [koGenModuleId] - this module's own generated `KoGenBeansFactoryImpl`/
 * `KoGenComponentsFactoryImpl`/`KoGenViewModelScopeImpl` classes, which the library can't know
 * about ahead of time - plus the tiny `inline`/`reified` wrappers needed to capture `T::class.java`
 * at each call site (a `reified` type parameter can only ever be captured by an `inline` function
 * *at* the call site, so this much can never be moved into the library itself).
 */
class InjectFactoryGenerator(
    private val packageName: String,
) {
    private val koGenModuleIdClass = ClassName("kz.evko.kogen_di.injector", "KoGenModuleId")
    private val contextClass = ClassName("android.content", "Context")
    private val beansFactoryImplClass = ClassName(packageName, "KoGenBeansFactoryImpl")
    private val componentsFactoryImplClass = ClassName(packageName, "KoGenComponentsFactoryImpl")
    private val viewModelScopeImplClass = ClassName(packageName, "KoGenViewModelScopeImpl")

    private val viewModelClass = ClassName("androidx.lifecycle", "ViewModel")
    private val viewModelStoreOwnerClass = ClassName("androidx.lifecycle", "ViewModelStoreOwner")
    private val composableAnnotation = ClassName("androidx.compose.runtime", "Composable")
    private val creationExtrasClass = ClassName("androidx.lifecycle.viewmodel", "CreationExtras")
    private val readOnlyPropertyClass = ClassName("kotlin.properties", "ReadOnlyProperty")
    private val fragmentClass = ClassName("androidx.fragment.app", "Fragment")
    private val componentActivityClass = ClassName("androidx.activity", "ComponentActivity")

    private val injectMember = MemberName("kz.evko.kogen_di.injector", "inject")
    private val setApplicationContextMember = MemberName("kz.evko.kogen_di.injector", "setApplicationContext")
    private val koGenViewModelMember = MemberName("kz.evko.kogen_di.viewModel", "koGenViewModel")

    /**
     * @param includeViewModelInjector Adds the Compose `koGenViewModel()`.
     * @param includeFragmentInjector Adds the `Fragment`/`ComponentActivity` `koGenViewModel()` delegate property.
     */
    fun generateInjectors(
        includeViewModelInjector: Boolean,
        includeFragmentInjector: Boolean,
    ): FileSpec {
        val fileBuilder = FileSpec.builder(packageName, "KoGenInjectors")
            .addProperty(buildModuleIdProperty())
            .addFunction(buildInjectFun())
            .addFunction(buildSetApplicationContextFun())

        if (includeViewModelInjector) {
            fileBuilder.addFunction(buildComposeViewModelFun())
        }

        if (includeFragmentInjector) {
            fileBuilder
                .addFunction(buildActivityExtensionFun(fragmentClass))
                .addFunction(buildActivityExtensionFun(componentActivityClass))
        }

        return fileBuilder.build()
    }

    /**
     * This module's own generated factory/scope classes, wrapped once as a `@PublishedApi
     * internal` constant - every entry point below just forwards to the library's own extension
     * function on it. `viewModelScopeClass` is always set (not gated on the two ViewModel KSP
     * options): `KoGenViewModelScopeImpl` is generated whenever *any*
     * `@KoGenComponent`/`@KoGenBean`/`@KoGenViewModel` exists in the module at all, regardless of
     * whether ViewModel support is enabled for it.
     */
    private fun buildModuleIdProperty(): PropertySpec {
        val initializer = CodeBlock.builder()
            .add("%T(\n", koGenModuleIdClass)
            .indent()
            .addStatement("scopeId = %S,", packageName)
            .addStatement("beansFactoryClass = %T::class.java,", beansFactoryImplClass)
            .addStatement("componentsFactoryClass = %T::class.java,", componentsFactoryImplClass)
            .addStatement("viewModelScopeClass = %T::class.java,", viewModelScopeImplClass)
            .unindent()
            .add(")")
            .build()

        return PropertySpec.builder("koGenModuleId", koGenModuleIdClass)
            .addAnnotation(ClassName("kotlin", "PublishedApi"))
            .addModifiers(KModifier.INTERNAL)
            .initializer(initializer)
            .build()
    }

    private fun buildInjectFun(): FunSpec {
        val reifiedT = TypeVariableName("T")
        return FunSpec.builder("inject")
            .addKdoc(
                """
                |Resolves [T] from the DI graph - a `@KoGenComponent`/`@KoGenBean`-provided
                |instance, or the registered application `Context` itself if [T] is `Context`.
                |
                |@param qualifier Matches a `@KoGenComponent`/`@KoGenBean`'s own `qualifier`
                |  argument - `""` (the default) requests the unqualified provider.
                |@throws kz.evko.kogen_di.exceptions.ComponentNotFoundException if nothing provides [T]
                |  under [qualifier].
                |@throws kz.evko.kogen_di.exceptions.ContextNotFoundException if [T] is `Context` and
                |  `setApplicationContext` hasn't been called yet.
                """.trimMargin(),
            )
            .addModifiers(KModifier.INLINE)
            .addTypeVariable(reifiedT.copy(reified = true))
            .addParameter(
                ParameterSpec.builder("qualifier", String::class)
                    .defaultValue("%S", "")
                    .build()
            )
            .returns(reifiedT)
            .addStatement("return koGenModuleId.%M(qualifier)", injectMember)
            .build()
    }

    private fun buildSetApplicationContextFun(): FunSpec {
        return FunSpec.builder("setApplicationContext")
            .addKdoc(
                """
                |Registers [context] as the application `Context` that `inject<Context>()` returns.
                |Call this once, before the first `inject()`/`koGenViewModel()` call - typically from
                |`Application.onCreate()`.
                """.trimMargin(),
            )
            .addParameter("context", contextClass)
            .addStatement("koGenModuleId.%M(context)", setApplicationContextMember)
            .build()
    }

    private fun buildComposeViewModelFun(): FunSpec {
        val reifiedT = TypeVariableName("T", viewModelClass)
        return FunSpec.builder("koGenViewModel")
            .addKdoc(
                """
                |Obtains a `@KoGenViewModel`-annotated [T], scoped to the current
                |`LocalViewModelStoreOwner` - this module's equivalent of `by viewModels()`, backed
                |by KoGen's own DI graph instead of a hand-written `ViewModelProvider.Factory`.
                |
                |@throws IllegalStateException if there's no `LocalViewModelStoreOwner` in scope.
                """.trimMargin(),
            )
            .addAnnotation(composableAnnotation)
            .addModifiers(KModifier.INLINE)
            .addTypeVariable(reifiedT.copy(reified = true))
            .returns(reifiedT)
            .addStatement("return koGenModuleId.%M()", koGenViewModelMember)
            .build()
    }

    private fun buildActivityExtensionFun(extensionClassName: ClassName): FunSpec {
        val reifiedT = TypeVariableName("T", viewModelClass)
        val extrasProducerType = LambdaTypeName.get(returnType = creationExtrasClass).copy(nullable = true)
        val ownerProducerType = LambdaTypeName.get(returnType = viewModelStoreOwnerClass)
        val returnPropertyType = readOnlyPropertyClass.parameterizedBy(extensionClassName, reifiedT)

        return FunSpec.builder("koGenViewModel")
            .addKdoc(
                """
                |Lazily obtains a `@KoGenViewModel`-annotated [T] scoped to [ownerProducer]'s
                |`ViewModelStore` - this type's equivalent of AndroidX's `by viewModels()`, backed
                |by KoGen's own DI graph.
                |
                |@param extrasProducer Accepted only to match `by viewModels()`'s call shape - KoGen's
                |  own `ViewModelProvider.Factory` doesn't use `CreationExtras`, so this is ignored.
                |@param ownerProducer The `ViewModelStoreOwner` [T] is scoped to. Defaults to the
                |  receiver itself.
                """.trimMargin(),
            )
            .receiver(extensionClassName)
            .addModifiers(KModifier.INLINE)
            .addTypeVariable(reifiedT.copy(reified = true))
            .addParameter(
                ParameterSpec.builder("extrasProducer", extrasProducerType)
                    .addModifiers(KModifier.NOINLINE)
                    .defaultValue("null")
                    .build()
            )
            .addParameter(
                ParameterSpec.builder("ownerProducer", ownerProducerType)
                    .addModifiers(KModifier.NOINLINE)
                    .defaultValue("{ this }")
                    .build()
            )
            .returns(returnPropertyType)
            .addStatement(
                "return koGenModuleId.%M(this, extrasProducer, ownerProducer)",
                koGenViewModelMember,
            )
            .build()
    }
}
