package kz.evko.kogen_di.validation

import com.google.devtools.ksp.symbol.KSDeclaration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * Изолированные тесты [DependencyValidator] - без реальной KSP-компиляции,
 * прямо на построенных вручную [ProviderNode].
 *
 * Оба реально найденных бага (bean не регистрировался как provider,
 * ambiguous-check ложно срабатывал на неиспользуемых общих supertype)
 * закреплены здесь регрессионными тестами, чтобы больше не воскресали
 * незамеченными.
 */
class DependencyValidatorTest {

    /** [ProviderNode.sourceElement] валидатору не интересен - только передаётся в лог. */
    private fun fakeSource(): KSDeclaration = mock(KSDeclaration::class.java)

    private fun componentNode(
        concreteType: String,
        requires: List<String> = emptyList(),
        satisfies: List<String> = listOf(concreteType),
    ) = ProviderNode(
        concreteType = concreteType,
        requiredDependencies = requires,
        satisfiableTypes = satisfies,
        sourceElement = fakeSource(),
    )

    /** Провайдер вроде @KoGenBean - сам НЕ входит в свой satisfiableTypes (в отличие от компонента). */
    private fun beanNode(
        functionName: String,
        returnType: String,
        requires: List<String> = emptyList(),
    ) = ProviderNode(
        concreteType = functionName,
        requiredDependencies = requires,
        satisfiableTypes = listOf(returnType),
        sourceElement = fakeSource(),
    )

    @Test
    fun `empty graph produces no errors`() {
        val logger = FakeKSPLogger()

        DependencyValidator(emptyList(), logger).validate()

        assertTrue(logger.errors.isEmpty())
    }

    @Test
    fun `dependency satisfied by exact concrete type produces no error`() {
        val logger = FakeKSPLogger()
        val providers = listOf(
            componentNode("com.app.Repository"),
            componentNode("com.app.UseCase", requires = listOf("com.app.Repository")),
        )

        DependencyValidator(providers, logger).validate()

        assertTrue("expected no errors but got: ${logger.errors}", logger.errors.isEmpty())
    }

    @Test
    fun `dependency satisfied through a declared supertype produces no error`() {
        val logger = FakeKSPLogger()
        val providers = listOf(
            componentNode(
                "com.app.RepositoryImpl",
                satisfies = listOf("com.app.RepositoryImpl", "com.app.Repository"),
            ),
            componentNode("com.app.UseCase", requires = listOf("com.app.Repository")),
        )

        DependencyValidator(providers, logger).validate()

        assertTrue("expected no errors but got: ${logger.errors}", logger.errors.isEmpty())
    }

    @Test
    fun `missing dependency is reported when nothing provides the required type`() {
        val logger = FakeKSPLogger()
        val providers = listOf(
            componentNode("com.app.UseCase", requires = listOf("com.app.Repository")),
        )

        DependencyValidator(providers, logger).validate()

        assertEquals(1, logger.errors.size)
        assertTrue(logger.errors[0].contains("Missing dependency"))
        assertTrue(logger.errors[0].contains("com.app.Repository"))
        assertTrue(logger.errors[0].contains("com.app.UseCase"))
    }

    @Test
    fun `android content Context is never reported as missing`() {
        val logger = FakeKSPLogger()
        val providers = listOf(
            componentNode("com.app.Repository", requires = listOf("android.content.Context")),
        )

        DependencyValidator(providers, logger).validate()

        assertTrue(logger.errors.isEmpty())
    }

    // --- regression: bean-provided type used to be silently invisible to the validator ---

    @Test
    fun `dependency satisfied by a KoGenBean-provided type produces no error`() {
        val logger = FakeKSPLogger()
        val providers = listOf(
            beanNode("com.app.provideApiService", returnType = "com.app.ApiService"),
            componentNode("com.app.Repository", requires = listOf("com.app.ApiService")),
        )

        DependencyValidator(providers, logger).validate()

        assertTrue("expected no errors but got: ${logger.errors}", logger.errors.isEmpty())
    }

    @Test
    fun `a KoGenBean own missing dependency is still reported`() {
        val logger = FakeKSPLogger()
        val providers = listOf(
            beanNode(
                "com.app.provideApiService",
                returnType = "com.app.ApiService",
                requires = listOf("com.app.MissingConfig"),
            ),
        )

        DependencyValidator(providers, logger).validate()

        assertEquals(1, logger.errors.size)
        assertTrue(logger.errors[0].contains("com.app.MissingConfig"))
    }

    // --- regression: sharing a common supertype used to be flagged even when nobody asked for it ---

    @Test
    fun `two providers sharing a common supertype are not ambiguous when nobody requests that type`() {
        val logger = FakeKSPLogger()
        val providers = listOf(
            componentNode(
                "com.app.MarkerImplA",
                satisfies = listOf("com.app.MarkerImplA", "com.app.SharedMarker"),
            ),
            componentNode(
                "com.app.MarkerImplB",
                satisfies = listOf("com.app.MarkerImplB", "com.app.SharedMarker"),
            ),
        )

        DependencyValidator(providers, logger).validate()

        assertTrue("expected no errors but got: ${logger.errors}", logger.errors.isEmpty())
    }

    @Test
    fun `two providers of a type that is actually requested are reported as ambiguous`() {
        val logger = FakeKSPLogger()
        val providers = listOf(
            componentNode(
                "com.app.MarkerImplA",
                satisfies = listOf("com.app.MarkerImplA", "com.app.SharedMarker"),
            ),
            componentNode(
                "com.app.MarkerImplB",
                satisfies = listOf("com.app.MarkerImplB", "com.app.SharedMarker"),
            ),
            componentNode("com.app.Consumer", requires = listOf("com.app.SharedMarker")),
        )

        DependencyValidator(providers, logger).validate()

        assertEquals(1, logger.errors.size)
        assertTrue(logger.errors[0].contains("Ambiguous dependency"))
        assertTrue(logger.errors[0].contains("com.app.SharedMarker"))
        assertTrue(logger.errors[0].contains("com.app.MarkerImplA"))
        assertTrue(logger.errors[0].contains("com.app.MarkerImplB"))
    }

    @Test
    fun `ignored ambiguity types are never flagged even when shared and requested`() {
        val logger = FakeKSPLogger()
        val providers = listOf(
            componentNode(
                "com.app.FirstViewModel",
                satisfies = listOf("com.app.FirstViewModel", "androidx.lifecycle.ViewModel"),
            ),
            componentNode(
                "com.app.SecondViewModel",
                satisfies = listOf("com.app.SecondViewModel", "androidx.lifecycle.ViewModel"),
            ),
            componentNode("com.app.Consumer", requires = listOf("androidx.lifecycle.ViewModel")),
        )

        DependencyValidator(providers, logger).validate()

        assertTrue("expected no errors but got: ${logger.errors}", logger.errors.isEmpty())
    }

    @Test
    fun `missing dependency check short-circuits before the ambiguous check runs`() {
        val logger = FakeKSPLogger()
        val providers = listOf(
            // genuinely ambiguous - two providers of the same requested type
            componentNode(
                "com.app.MarkerImplA",
                satisfies = listOf("com.app.MarkerImplA", "com.app.SharedMarker"),
            ),
            componentNode(
                "com.app.MarkerImplB",
                satisfies = listOf("com.app.MarkerImplB", "com.app.SharedMarker"),
            ),
            componentNode("com.app.Consumer", requires = listOf("com.app.SharedMarker")),
            // ...but also a plain missing dependency elsewhere in the same graph
            componentNode("com.app.Broken", requires = listOf("com.app.DoesNotExist")),
        )

        DependencyValidator(providers, logger).validate()

        assertEquals(
            "only the missing-dependency error should be reported, ambiguous check must not run",
            1,
            logger.errors.size,
        )
        assertTrue(logger.errors[0].contains("Missing dependency"))
    }

    @Test
    fun `a larger valid graph with beans, components and shared supertypes produces zero errors`() {
        val logger = FakeKSPLogger()
        val providers = listOf(
            beanNode("com.app.provideApiService", returnType = "com.app.ApiService"),
            componentNode(
                "com.app.RepositoryImpl",
                satisfies = listOf("com.app.RepositoryImpl", "com.app.Repository"),
                requires = listOf("com.app.ApiService"),
            ),
            componentNode(
                "com.app.GetDataUseCaseImpl",
                satisfies = listOf("com.app.GetDataUseCaseImpl", "com.app.GetDataUseCase"),
                requires = listOf("com.app.Repository"),
            ),
            componentNode(
                "com.app.RefreshDataUseCaseImpl",
                satisfies = listOf("com.app.RefreshDataUseCaseImpl", "com.app.RefreshDataUseCase"),
                requires = listOf("com.app.Repository"),
            ),
            componentNode(
                "com.app.ClassicViewModel",
                satisfies = listOf("com.app.ClassicViewModel", "androidx.lifecycle.ViewModel"),
                requires = listOf("com.app.GetDataUseCase", "com.app.RefreshDataUseCase"),
            ),
        )

        DependencyValidator(providers, logger).validate()

        assertTrue("expected no errors but got: ${logger.errors}", logger.errors.isEmpty())
    }
}
