package kz.evko.kogen_di

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspSourcesDir
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)

/**
 * Тесты полного пайплайна KoGenProvider через реальную KSP-компиляцию
 * (kotlin-compile-testing), а не через мокнутые символы или demo-приложение.
 *
 * Это единственный уровень, на котором можно проверить, что заведомо
 * некорректный код (missing/ambiguous dependency) реально ПАДАЕТ на сборке -
 * такое нельзя постоянно держать в demo-приложении, оно бы просто не собиралось.
 */
class KoGenProviderCompileTest {

    private class Compiled(val compilation: KotlinCompilation, val result: JvmCompilationResult)

    // koGenDi-compiler - и поэтому эта тестовая JVM-компиляция - не тянет Android SDK на
    // classpath, а KoGenInjectors.kt всегда ссылается на android.content.Context (нужен для
    // inject()/setApplicationContext()). Даём компиляции минимальную заглушку вместо реального
    // android.jar - тип нужен только для компиляции сигнатур, ни один метод не вызывается.
    private val androidContextStub = SourceFile.kotlin(
        "Context.kt",
        """
        package android.content
        open class Context
        """.trimIndent(),
    )

    // koGenDi-compiler больше не зависит от koGenDi (тот теперь Android-библиотека, а
    // plain JVM модуль не может резолвить AAR-проект как зависимость). Но сгенерённый
    // код реально ссылается на классы рантайма (KoGenScope и т.д.), поэтому даём
    // компиляции минимальные самодостаточные заглушки с тем же публичным API.
    private val runtimeInjectorStub = SourceFile.kotlin(
        "KoGenInjectorStubs.kt",
        """
        package kz.evko.kogen_di.injector

        data class KoGenKey(val type: Class<*>, val qualifier: String = "")

        class KoGenScope {
            val applicationContext: Any? = null
            fun getComponent(reference: Class<*>, qualifier: String = ""): Any = Any()

            companion object {
                fun getScope(
                    scopeId: String,
                    beansFactoryClass: Class<out KoGenBeansFactory>,
                    componentsFactoryClass: Class<out KoGenComponentsFactory>,
                ): KoGenScope = KoGenScope()

                fun setApplicationContext(
                    scopeId: String,
                    context: Any,
                    beansFactoryClass: Class<out KoGenBeansFactory>,
                    componentsFactoryClass: Class<out KoGenComponentsFactory>,
                ) = Unit
            }
        }

        interface KoGenBeans {
            val singleton: Boolean
            fun getComponentObject(): Any
        }

        abstract class KoGenBeansFactory {
            abstract fun createBeansList(): Map<KoGenKey, KoGenBeans>
        }

        interface KoGenComponents {
            val singleton: Boolean
            fun getComponentObject(): Any
        }

        abstract class KoGenComponentsFactory {
            abstract fun createComponentsMap(): Map<KoGenKey, KoGenComponents>
        }

        data class KoGenModuleId(
            val scopeId: String,
            val beansFactoryClass: Class<out KoGenBeansFactory>,
            val componentsFactoryClass: Class<out KoGenComponentsFactory>,
            val viewModelScopeClass: Class<out kz.evko.kogen_di.viewModel.KoGenViewModelScope>,
        )

        inline fun <reified T> KoGenModuleId.inject(qualifier: String = ""): T = Any() as T

        fun KoGenModuleId.setApplicationContext(context: Any) = Unit
        """.trimIndent(),
    )

    private val runtimeViewModelStub = SourceFile.kotlin(
        "KoGenViewModelStubs.kt",
        """
        package kz.evko.kogen_di.viewModel

        interface KoGenViewModels {
            fun getComponentObject(): Any
        }

        abstract class KoGenViewModelScope {
            abstract fun createViewModelsMap(): Map<Class<*>, KoGenViewModels>

            companion object {
                fun getInstance(
                    scopeId: String,
                    reference: Class<out KoGenViewModelScope>,
                ): KoGenViewModelScope = object : KoGenViewModelScope() {
                    override fun createViewModelsMap(): Map<Class<*>, KoGenViewModels> = emptyMap()
                }
            }
        }
        """.trimIndent(),
    )

    private fun compile(source: String): Compiled {
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin("Source.kt", source),
                androidContextStub,
                runtimeInjectorStub,
                runtimeViewModelStub,
            )
            inheritClassPath = true
            messageOutputStream = System.out
            configureKsp(useKsp2 = true) {
                symbolProcessorProviders += KoGenProvider()
                processorOptions["packageName"] = "com.test.generated"
            }
        }
        return Compiled(compilation, compilation.compile())
    }

    private fun Compiled.generatedFile(name: String): String {
        val allFiles = compilation.kspSourcesDir.walkTopDown().toList()
        val file = allFiles.firstOrNull { it.name == name }
        assertTrue(
            "generated file '$name' not found among: ${allFiles.map { it.name }}",
            file != null,
        )
        return file!!.readText()
    }

    @Test
    fun `simple component with no dependencies compiles and is generated`() {
        val compiled = compile(
            """
            package com.test
            import kz.evko.kogen_di.annotations.KoGenComponent

            @KoGenComponent
            class SimpleService {
                fun ping(): String = "pong"
            }
            """.trimIndent()
        )

        assertEquals(KotlinCompilation.ExitCode.OK, compiled.result.exitCode)
        val generated = compiled.generatedFile("KoGenComponentsImpl.kt")
        assertTrue(generated.contains("SimpleService"))
    }

    @Test
    fun `KoGenInjectors only carries a per-module id, every entry point delegates to the koGenDi library`() {
        // the actual inject()/setApplicationContext() logic now lives in the koGenDi runtime
        // library (KoGenInjection.kt) - what's generated per module should shrink to just the id
        // plus one-line delegates, not reimplement the logic itself.
        val compiled = compile(
            """
            package com.test
            import kz.evko.kogen_di.annotations.KoGenComponent

            @KoGenComponent
            class SimpleService
            """.trimIndent()
        )

        assertEquals(KotlinCompilation.ExitCode.OK, compiled.result.exitCode)
        val generated = compiled.generatedFile("KoGenInjectors.kt")
        assertTrue(generated.contains("import kz.evko.kogen_di.injector.inject"))
        assertTrue(generated.contains("import kz.evko.kogen_di.injector.setApplicationContext"))
        assertTrue(generated.contains("internal val koGenModuleId"))
        assertTrue(generated.contains("KoGenModuleId("))
        assertTrue(generated.contains("beansFactoryClass = KoGenBeansFactoryImpl::class.java"))
        assertTrue(generated.contains("componentsFactoryClass = KoGenComponentsFactoryImpl::class.java"))
        assertTrue(generated.contains("viewModelScopeClass = KoGenViewModelScopeImpl::class.java"))
        assertTrue(generated.contains("= koGenModuleId.inject(qualifier)"))
        assertTrue(generated.contains("koGenModuleId.setApplicationContext(context)"))
        // no ViewModel/Compose entry points without the KSP flags that opt into them
        assertTrue(!generated.contains("fun koGenViewModel"))
    }

    @Test
    fun `missing dependency fails compilation with a clear error`() {
        val compiled = compile(
            """
            package com.test
            import kz.evko.kogen_di.annotations.KoGenComponent

            interface Repository
            @KoGenComponent
            class UseCase(private val repository: Repository)
            """.trimIndent()
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, compiled.result.exitCode)
        assertTrue(compiled.result.messages.contains("Missing dependency"))
        assertTrue(compiled.result.messages.contains("com.test.Repository"))
    }

    @Test
    fun `ambiguous dependency fails compilation with a clear error`() {
        val compiled = compile(
            """
            package com.test
            import kz.evko.kogen_di.annotations.KoGenComponent

            interface Marker
            @KoGenComponent
            class MarkerImplA : Marker
            @KoGenComponent
            class MarkerImplB : Marker
            @KoGenComponent
            class Consumer(private val marker: Marker)
            """.trimIndent()
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, compiled.result.exitCode)
        assertTrue(compiled.result.messages.contains("Ambiguous dependency"))
        assertTrue(compiled.result.messages.contains("com.test.Marker"))
    }

    @Test
    fun `two components sharing a common supertype compile fine when nobody requests that type`() {
        // regression: the old ambiguous-check flagged ANY shared supertype, even unused ones
        val compiled = compile(
            """
            package com.test
            import kz.evko.kogen_di.annotations.KoGenComponent

            interface Marker
            @KoGenComponent
            class MarkerImplA : Marker
            @KoGenComponent
            class MarkerImplB : Marker
            """.trimIndent()
        )

        assertEquals(KotlinCompilation.ExitCode.OK, compiled.result.exitCode)
    }

    @Test
    fun `component depending on a KoGenBean-provided type compiles successfully end-to-end`() {
        // regression: @KoGenBean used to be invisible to the dependency validator
        val compiled = compile(
            """
            package com.test
            import kz.evko.kogen_di.annotations.KoGenBean
            import kz.evko.kogen_di.annotations.KoGenComponent

            interface ApiService
            class ApiServiceImpl : ApiService

            @KoGenBean(singleton = true)
            fun provideApiService(): ApiService = ApiServiceImpl()

            @KoGenComponent
            class Repository(private val apiService: ApiService)
            """.trimIndent()
        )

        assertEquals(KotlinCompilation.ExitCode.OK, compiled.result.exitCode)
        val beans = compiled.generatedFile("KoGenBeansImpl.kt")
        assertTrue(beans.contains("singleton = true"))
        val components = compiled.generatedFile("KoGenComponentsImpl.kt")
        assertTrue(components.contains("apiService = inject()"))
    }

    @Test
    fun `component satisfying multiple supertypes is resolvable under all of them`() {
        val compiled = compile(
            """
            package com.test
            import kz.evko.kogen_di.annotations.KoGenComponent

            interface Named
            interface Aged
            @KoGenComponent
            class PersonImpl : Named, Aged
            """.trimIndent()
        )

        assertEquals(KotlinCompilation.ExitCode.OK, compiled.result.exitCode)
        // с переходом на Class-identity lookup карта "тип -> компонент" живёт в
        // KoGenComponentsFactoryImpl, а не в самом enum (KoGenComponentsImpl)
        val factory = compiled.generatedFile("KoGenComponentsFactoryImpl.kt")
        assertTrue(factory.contains("PersonImpl::class.java"))
        assertTrue(factory.contains("Named::class.java"))
        assertTrue(factory.contains("Aged::class.java"))
    }

    @Test
    fun `component satisfying an indirect (grand-parent) supertype is still resolvable`() {
        // регрессия на баг, найденный при переделке: satisfiableNames раньше не
        // рекурсировала дальше прямых supertype
        val compiled = compile(
            """
            package com.test
            import kz.evko.kogen_di.annotations.KoGenComponent

            interface GrandParent
            interface Parent : GrandParent
            @KoGenComponent
            class ChildImpl : Parent
            """.trimIndent()
        )

        assertEquals(KotlinCompilation.ExitCode.OK, compiled.result.exitCode)
        val factory = compiled.generatedFile("KoGenComponentsFactoryImpl.kt")
        assertTrue(factory.contains("ChildImpl::class.java"))
        assertTrue(factory.contains("Parent::class.java"))
        assertTrue(factory.contains("GrandParent::class.java"))
    }

    @Test
    fun `two components of the same type under different qualifiers compile without an ambiguous-dependency error`() {
        // regression guard: qualifier is meant to make otherwise-ambiguous same-type providers
        // coexist, exactly like Koin's named qualifiers - neither should trip the ambiguous check
        // as long as nothing requires that type unqualified.
        val compiled = compile(
            """
            package com.test
            import kz.evko.kogen_di.annotations.KoGenComponent

            interface Repository
            @KoGenComponent(qualifier = "primary")
            class PrimaryRepository : Repository
            @KoGenComponent(qualifier = "secondary")
            class SecondaryRepository : Repository
            """.trimIndent()
        )

        assertEquals(KotlinCompilation.ExitCode.OK, compiled.result.exitCode)
        val factory = compiled.generatedFile("KoGenComponentsFactoryImpl.kt")
        assertTrue(factory.contains("KoGenKey(Repository::class.java, \"primary\")"))
        assertTrue(factory.contains("KoGenKey(Repository::class.java, \"secondary\")"))
    }

    @Test
    fun `a qualified bean is invisible to an unqualified dependency, reported as missing`() {
        // the only provider of ApiService is qualifier = "alt", so the unqualified constructor
        // parameter below must still fail exactly like no provider existed at all.
        val compiled = compile(
            """
            package com.test
            import kz.evko.kogen_di.annotations.KoGenBean
            import kz.evko.kogen_di.annotations.KoGenComponent

            interface ApiService
            class ApiServiceImpl : ApiService

            @KoGenBean(qualifier = "alt")
            fun provideApiService(): ApiService = ApiServiceImpl()

            @KoGenComponent
            class Repository(private val apiService: ApiService)
            """.trimIndent()
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, compiled.result.exitCode)
        assertTrue(compiled.result.messages.contains("Missing dependency"))
        assertTrue(compiled.result.messages.contains("com.test.ApiService"))
    }

    @Test
    fun `a constructor parameter annotated with KoGenQualifier resolves the matching qualified provider`() {
        val compiled = compile(
            """
            package com.test
            import kz.evko.kogen_di.annotations.KoGenComponent
            import kz.evko.kogen_di.annotations.KoGenQualifier

            interface Repository
            @KoGenComponent(qualifier = "primary")
            class PrimaryRepository : Repository
            @KoGenComponent(qualifier = "secondary")
            class SecondaryRepository : Repository

            @KoGenComponent
            class SyncService(@KoGenQualifier("primary") private val repository: Repository)
            """.trimIndent()
        )

        assertEquals(KotlinCompilation.ExitCode.OK, compiled.result.exitCode)
        val components = compiled.generatedFile("KoGenComponentsImpl.kt")
        assertTrue(components.contains("repository = inject(qualifier = \"primary\")"))
    }

    @Test
    fun `a constructor parameter requesting a qualifier nobody provides fails compilation as missing`() {
        val compiled = compile(
            """
            package com.test
            import kz.evko.kogen_di.annotations.KoGenComponent
            import kz.evko.kogen_di.annotations.KoGenQualifier

            interface Repository
            @KoGenComponent
            class DefaultRepository : Repository

            @KoGenComponent
            class SyncService(@KoGenQualifier("primary") private val repository: Repository)
            """.trimIndent()
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, compiled.result.exitCode)
        assertTrue(compiled.result.messages.contains("Missing dependency"))
        assertTrue(compiled.result.messages.contains("com.test.Repository"))
    }

    @Test
    fun `two providers under the same requested qualifier fail compilation as duplicates`() {
        val compiled = compile(
            """
            package com.test
            import kz.evko.kogen_di.annotations.KoGenComponent
            import kz.evko.kogen_di.annotations.KoGenQualifier

            interface Repository
            @KoGenComponent(qualifier = "primary")
            class RepositoryA : Repository
            @KoGenComponent(qualifier = "primary")
            class RepositoryB : Repository

            @KoGenComponent
            class SyncService(@KoGenQualifier("primary") private val repository: Repository)
            """.trimIndent()
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, compiled.result.exitCode)
        assertTrue(compiled.result.messages.contains("Duplicate provider"))
        assertTrue(compiled.result.messages.contains("com.test.Repository"))
    }

    @Test
    fun `two providers under the same non-empty qualifier fail compilation even when unrequested`() {
        // the duplicate-provider check is unconditional for a non-empty qualifier - unlike the
        // empty-qualifier case (see the "sharing a common supertype" test above), it doesn't wait
        // for something to actually request it.
        val compiled = compile(
            """
            package com.test
            import kz.evko.kogen_di.annotations.KoGenComponent

            interface Repository
            @KoGenComponent(qualifier = "primary")
            class RepositoryA : Repository
            @KoGenComponent(qualifier = "primary")
            class RepositoryB : Repository
            """.trimIndent()
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, compiled.result.exitCode)
        assertTrue(compiled.result.messages.contains("Duplicate provider"))
        assertTrue(compiled.result.messages.contains("com.test.Repository"))
    }

    @Test
    fun `KoGenViewModel with dependencies compiles and is generated`() {
        // не наследуем androidx.lifecycle.ViewModel - генератору это не нужно
        // (только @KoGenViewModel + первичный конструктор), а тащить ещё один
        // Android-стаб ради этого теста не имеет смысла
        val compiled = compile(
            """
            package com.test
            import kz.evko.kogen_di.annotations.KoGenComponent
            import kz.evko.kogen_di.annotations.KoGenViewModel

            @KoGenComponent
            class UseCase

            @KoGenViewModel
            class MainViewModel(private val useCase: UseCase)
            """.trimIndent(),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, compiled.result.exitCode)
        val viewModels = compiled.generatedFile("KoGenViewModelsImpl.kt")
        assertTrue(viewModels.contains("com.test.MainViewModel"))
        assertTrue(viewModels.contains("useCase = inject()"))
    }
}
