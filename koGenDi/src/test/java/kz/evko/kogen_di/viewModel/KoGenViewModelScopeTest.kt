package kz.evko.kogen_di.viewModel

import kz.evko.kogen_di.exceptions.ViewModelNotRegisteredException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KoGenViewModelScopeTest {

    class FakeViewModel

    private class FakeEntry(private val factory: () -> Any) : KoGenViewModels {
        override fun getComponentObject(): Any = factory()
    }

    private class TestScope(
        private val map: Map<Class<*>, KoGenViewModels>,
    ) : KoGenViewModelScope() {
        var createCalls = 0
        override fun createViewModelsMap(): Map<Class<*>, KoGenViewModels> {
            createCalls++
            return map
        }
    }

    @Test
    fun `getViewModel constructs and returns the registered view model`() {
        val viewModel = FakeViewModel()
        val scope = TestScope(mapOf(FakeViewModel::class.java to FakeEntry { viewModel }))

        val result = scope.getViewModel(FakeViewModel::class.java)

        assertSame(viewModel, result)
    }

    @Test(expected = ViewModelNotRegisteredException::class)
    fun `getViewModel throws for an unregistered type`() {
        val scope = TestScope(emptyMap())
        scope.getViewModel(FakeViewModel::class.java)
    }

    @Test
    fun `getViewModel constructs a fresh instance on every call - no caching at this layer`() {
        // KoGenViewModelScope сам не кэширует - реальное кэширование (по жизненному циклу
        // экрана) делает androidx.lifecycle.ViewModelProvider выше по стеку, не эта функция.
        var creations = 0
        val scope = TestScope(
            mapOf(FakeViewModel::class.java to FakeEntry { creations++; FakeViewModel() })
        )

        scope.getViewModel(FakeViewModel::class.java)
        scope.getViewModel(FakeViewModel::class.java)

        assertEquals(2, creations)
    }

    @Test
    fun `createViewModelsMap is invoked lazily, only once, across many lookups`() {
        val scope = TestScope(mapOf(FakeViewModel::class.java to FakeEntry { FakeViewModel() }))

        scope.getViewModel(FakeViewModel::class.java)
        scope.getViewModel(FakeViewModel::class.java)

        assertEquals(1, scope.createCalls)
    }

    @Test
    fun `getInstance returns the same scope instance for the same scopeId`() {
        val first = KoGenViewModelScope.getInstance("test-scope-same", EmptyScopeForInstanceTest::class.java)
        val second = KoGenViewModelScope.getInstance("test-scope-same", EmptyScopeForInstanceTest::class.java)

        assertSame(first, second)
    }

    @Test
    fun `getInstance returns different instances for different scopeIds`() {
        val a = KoGenViewModelScope.getInstance("test-scope-diff-a", EmptyScopeForInstanceTest::class.java)
        val b = KoGenViewModelScope.getInstance("test-scope-diff-b", EmptyScopeForInstanceTest::class.java)

        assertTrue(a !== b)
    }

    class EmptyScopeForInstanceTest : KoGenViewModelScope() {
        override fun createViewModelsMap(): Map<Class<*>, KoGenViewModels> = emptyMap()
    }
}
