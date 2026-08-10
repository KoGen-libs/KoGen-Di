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
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName

/**
 * Builds `KoGenInjectors.kt` - the `inject()`/`setApplicationContext()` entry points every
 * consumer gets, plus (when the matching KSP option is on) a Compose `koGenViewModel()` and a
 * `Fragment`/`ComponentActivity` `koGenViewModel()` delegate property. Written once per KSP run,
 * regardless of whether anything is annotated - see [generateInjectors].
 */
class InjectFactoryGenerator(
    private val packageName: String,
) {
    private val koGenScopeClass = ClassName("kz.evko.kogen_di.injector", "KoGenScope")
    private val contextClass = ClassName("android.content", "Context")
    private val beansFactoryImplClass = ClassName(packageName, "KoGenBeansFactoryImpl")
    private val componentsFactoryImplClass = ClassName(packageName, "KoGenComponentsFactoryImpl")

    private val viewModelClass = ClassName("androidx.lifecycle", "ViewModel")
    private val koGenViewModelScopeClass = ClassName("kz.evko.kogen_di.viewModel", "KoGenViewModelScope")
    private val viewModelScopeImplClass = ClassName(packageName, "KoGenViewModelScopeImpl")
    private val viewModelFactoryClass = ClassName(packageName, "KoGenViewModelFactory")
    private val viewModelProviderClass = ClassName("androidx.lifecycle", "ViewModelProvider")
    private val viewModelStoreOwnerClass = ClassName("androidx.lifecycle", "ViewModelStoreOwner")
    private val composableAnnotation = ClassName("androidx.compose.runtime", "Composable")
    private val localViewModelStoreOwnerMember =
        MemberName("androidx.lifecycle.viewmodel.compose", "LocalViewModelStoreOwner")
    private val currentComposerMember = MemberName("androidx.compose.runtime", "currentComposer")
    private val rememberMember = MemberName("androidx.compose.runtime", "remember")

    private val creationExtrasClass = ClassName("androidx.lifecycle.viewmodel", "CreationExtras")
    private val readOnlyPropertyClass = ClassName("kotlin.properties", "ReadOnlyProperty")
    private val kPropertyClass = ClassName("kotlin.reflect", "KProperty")
    private val lazyThreadSafetyModeClass = ClassName("kotlin", "LazyThreadSafetyMode")
    private val fragmentClass = ClassName("androidx.fragment.app", "Fragment")
    private val componentActivityClass = ClassName("androidx.activity", "ComponentActivity")

    /**
     * @param includeViewModelInjector Adds the Compose `koGenViewModel()` and its backing `KoGenViewModelFactory`.
     * @param includeFragmentInjector Adds the `Fragment`/`ComponentActivity` `koGenViewModel()` delegate property.
     */
    fun generateInjectors(
        includeViewModelInjector: Boolean,
        includeFragmentInjector: Boolean,
    ): FileSpec {
        val fileBuilder = FileSpec.builder(packageName, "KoGenInjectors")
            .addFunction(buildInjectFun())
            .addFunction(buildSetApplicationContextFun())

        if (includeViewModelInjector) {
            fileBuilder
                .addFunction(buildComposeViewModelFun())
                .addType(buildViewModelFactorySpec())
        }

        if (includeFragmentInjector) {
            fileBuilder
                .addFunction(buildActivityExtensionFun(fragmentClass))
                .addFunction(buildActivityExtensionFun(componentActivityClass))
        }

        return fileBuilder.build()
    }

    private fun buildInjectFun(): FunSpec {
        val reifiedT = TypeVariableName("T")
        val body = CodeBlock.of(
            "val reference = %T::class.java\n" +
                "\n" +
                "%T.getScope(\n" +
                "\tscopeId = %S,\n" +
                "\tbeansFactoryClass = %T::class.java,\n" +
                "\tcomponentsFactoryClass = %T::class.java,\n" +
                ").run {\n" +
                "\tif (reference == %T::class.java) {\n" +
                "\t\treturn this.applicationContext as %T\n" +
                "\t}\n" +
                "\treturn this.getComponent(%T::class.java) as %T\n" +
                "}\n",
            reifiedT, koGenScopeClass, packageName, beansFactoryImplClass, componentsFactoryImplClass,
            contextClass, reifiedT, reifiedT, reifiedT,
        )
        return FunSpec.builder("inject")
            .addKdoc(
                """
                |Resolves [T] from the DI graph - a `@KoGenComponent`/`@KoGenBean`-provided
                |instance, or the registered application `Context` itself if [T] is `Context`.
                |
                |@throws kz.evko.kogen_di.exceptions.ComponentNotFoundException if nothing provides [T].
                |@throws kz.evko.kogen_di.exceptions.ContextNotFoundException if [T] is `Context` and
                |  `setApplicationContext` hasn't been called yet.
                """.trimMargin(),
            )
            .addModifiers(KModifier.INLINE)
            .addTypeVariable(reifiedT.copy(reified = true))
            .returns(reifiedT)
            .addCode(body)
            .build()
    }

    private fun buildSetApplicationContextFun(): FunSpec {
        val body = CodeBlock.of(
            "%T.setApplicationContext(\n" +
                "\tscopeId = %S,\n" +
                "\tcontext = context,\n" +
                "\tbeansFactoryClass = %T::class.java,\n" +
                "\tcomponentsFactoryClass = %T::class.java,\n" +
                ")\n",
            koGenScopeClass, packageName, beansFactoryImplClass, componentsFactoryImplClass,
        )
        return FunSpec.builder("setApplicationContext")
            .addKdoc(
                """
                |Registers [context] as the application `Context` that `inject<Context>()` returns.
                |Call this once, before the first `inject()`/`koGenViewModel()` call - typically from
                |`Application.onCreate()`.
                """.trimMargin(),
            )
            .addParameter("context", contextClass)
            .addCode(body)
            .build()
    }

    private fun buildComposeViewModelFun(): FunSpec {
        val reifiedT = TypeVariableName("T", viewModelClass)
        val body = CodeBlock.of(
            "val viewModelStoreOwner: %T = checkNotNull(\n" +
                "\t%M.current\n" +
                ") {\n" +
                "\t%S\n" +
                "}\n" +
                "\n" +
                "return %M.run {\n" +
                "\t%M {\n" +
                "\t\tval scope = %T.getInstance(\n" +
                "\t\t\tscopeId = %S,\n" +
                "\t\t\treference = %T::class.java,\n" +
                "\t\t)\n" +
                "\t\t%T(\n" +
                "\t\t\tstore = viewModelStoreOwner.viewModelStore,\n" +
                "\t\t\tfactory = %T(scope),\n" +
                "\t\t)[T::class.java]\n" +
                "\t}\n" +
                "}\n",
            viewModelStoreOwnerClass,
            localViewModelStoreOwnerMember,
            "No ViewModelStoreOwner was provided",
            currentComposerMember,
            rememberMember,
            koGenViewModelScopeClass,
            packageName,
            viewModelScopeImplClass,
            viewModelProviderClass,
            viewModelFactoryClass,
        )
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
            .addCode(body)
            .build()
    }

    private fun buildViewModelFactorySpec(): TypeSpec {
        val factoryInterface = viewModelProviderClass.nestedClass("Factory")
        val createTypeVar = TypeVariableName("T", viewModelClass)
        val classOfT = ClassName("java.lang", "Class").parameterizedBy(createTypeVar)

        val createFun = FunSpec.builder("create")
            .addTypeVariable(createTypeVar)
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("modelClass", classOfT)
            .returns(createTypeVar)
            .addStatement("return scope.getViewModel(modelClass) as %T", createTypeVar)
            .build()

        return TypeSpec.classBuilder("KoGenViewModelFactory")
            .addKdoc(
                """
                |`ViewModelProvider.Factory` that resolves a requested ViewModel through [scope]'s
                |DI graph instead of constructing it directly - what `koGenViewModel()` uses under
                |the hood.
                """.trimMargin(),
            )
            .addSuperinterface(factoryInterface)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("scope", koGenViewModelScopeClass)
                    .build()
            )
            .addProperty(
                PropertySpec.builder("scope", koGenViewModelScopeClass)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer("scope")
                    .build()
            )
            .addFunction(createFun)
            .build()
    }

    private fun buildActivityExtensionFun(extensionClassName: ClassName): FunSpec {
        val reifiedT = TypeVariableName("T", viewModelClass)
        val extrasProducerType = LambdaTypeName.get(returnType = creationExtrasClass).copy(nullable = true)
        val ownerProducerType = LambdaTypeName.get(returnType = viewModelStoreOwnerClass)
        val returnPropertyType = readOnlyPropertyClass.parameterizedBy(extensionClassName, reifiedT)

        val body = CodeBlock.of(
            "return lazy(%T.NONE) {\n" +
                "\tval scope = %T.getInstance(\n" +
                "\t\tscopeId = %S,\n" +
                "\t\treference = %T::class.java,\n" +
                "\t)\n" +
                "\t%T(\n" +
                "\t\towner = ownerProducer(),\n" +
                "\t\tfactory = %T(scope),\n" +
                "\t)[T::class.java]\n" +
                "}.let { lazyViewModel ->\n" +
                "\tobject : %T<%T, T> {\n" +
                "\t\toverride fun getValue(\n" +
                "\t\t\tthisRef: %T,\n" +
                "\t\t\tproperty: %T<*>,\n" +
                "\t\t): T {\n" +
                "\t\t\treturn lazyViewModel.value\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}\n",
            lazyThreadSafetyModeClass,
            koGenViewModelScopeClass,
            packageName,
            viewModelScopeImplClass,
            viewModelProviderClass,
            viewModelFactoryClass,
            readOnlyPropertyClass,
            extensionClassName,
            extensionClassName,
            kPropertyClass,
        )

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
            .addCode(body)
            .build()
    }
}
