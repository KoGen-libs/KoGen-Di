package kz.evko.kogen_di.injector

import kz.evko.kogen_di.exceptions.ComponentNotFoundException
import kz.evko.kogen_di.exceptions.ContextNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KoGenScopeTest {

    private class TestBeansFactory(private val map: Map<Class<*>, KoGenBeans>) : KoGenBeansFactory() {
        override fun createBeansList(): Map<Class<*>, KoGenBeans> = map
    }

    private class TestComponentsFactory(private val map: Map<Class<*>, KoGenComponents>) : KoGenComponentsFactory() {
        override fun createComponentsMap(): Map<Class<*>, KoGenComponents> = map
    }

    private class FakeBean(private val value: Any) : KoGenBeans {
        override val singleton = false
        override fun getComponentObject(): Any = value
    }

    private class FakeComponent(private val value: Any) : KoGenComponents {
        override val singleton = false
        override fun getComponentObject(): Any = value
    }

    @Test
    fun `getComponent resolves via the beans factory first`() {
        val beanValue = "from-bean"
        val scope = KoGenScope(
            beansFactory = TestBeansFactory(mapOf(String::class.java to FakeBean(beanValue))),
            componentsFactory = TestComponentsFactory(emptyMap()),
        )

        assertEquals(beanValue, scope.getComponent(String::class.java))
    }

    @Test
    fun `getComponent falls back to the components factory when no bean matches`() {
        val componentValue = "from-component"
        val scope = KoGenScope(
            beansFactory = TestBeansFactory(emptyMap()),
            componentsFactory = TestComponentsFactory(mapOf(String::class.java to FakeComponent(componentValue))),
        )

        assertEquals(componentValue, scope.getComponent(String::class.java))
    }

    @Test
    fun `getComponent prefers a bean over a component registered under the same type`() {
        val beanValue = "from-bean"
        val scope = KoGenScope(
            beansFactory = TestBeansFactory(mapOf(String::class.java to FakeBean(beanValue))),
            componentsFactory = TestComponentsFactory(mapOf(String::class.java to FakeComponent("from-component"))),
        )

        assertEquals(beanValue, scope.getComponent(String::class.java))
    }

    @Test(expected = ComponentNotFoundException::class)
    fun `getComponent throws when neither factory has the requested type`() {
        val scope = KoGenScope(
            beansFactory = TestBeansFactory(emptyMap()),
            componentsFactory = TestComponentsFactory(emptyMap()),
        )

        scope.getComponent(String::class.java)
    }

    @Test(expected = ContextNotFoundException::class)
    fun `applicationContext throws before setApplicationContext is ever called`() {
        val scope = KoGenScope(
            beansFactory = TestBeansFactory(emptyMap()),
            componentsFactory = TestComponentsFactory(emptyMap()),
        )

        scope.applicationContext
    }

    @Test
    fun `getScope returns the same instance for the same scopeId`() {
        val first = KoGenScope.getScope(
            scopeId = "test-getScope-same",
            beansFactoryClass = EmptyBeansFactory::class.java,
            componentsFactoryClass = EmptyComponentsFactory::class.java,
        )
        val second = KoGenScope.getScope(
            scopeId = "test-getScope-same",
            beansFactoryClass = EmptyBeansFactory::class.java,
            componentsFactoryClass = EmptyComponentsFactory::class.java,
        )

        assertSame(first, second)
    }

    @Test
    fun `getScope returns different instances for different scopeIds`() {
        val a = KoGenScope.getScope(
            scopeId = "test-getScope-diff-a",
            beansFactoryClass = EmptyBeansFactory::class.java,
            componentsFactoryClass = EmptyComponentsFactory::class.java,
        )
        val b = KoGenScope.getScope(
            scopeId = "test-getScope-diff-b",
            beansFactoryClass = EmptyBeansFactory::class.java,
            componentsFactoryClass = EmptyComponentsFactory::class.java,
        )

        assertTrue(a !== b)
    }

    @Test
    fun `setApplicationContext makes the context available through the same scope afterwards`() {
        val scopeId = "test-setApplicationContext"
        val context = Any()

        KoGenScope.setApplicationContext(
            scopeId = scopeId,
            context = context,
            beansFactoryClass = EmptyBeansFactory::class.java,
            componentsFactoryClass = EmptyComponentsFactory::class.java,
        )

        val scope = KoGenScope.getScope(
            scopeId = scopeId,
            beansFactoryClass = EmptyBeansFactory::class.java,
            componentsFactoryClass = EmptyComponentsFactory::class.java,
        )

        assertSame(context, scope.applicationContext)
    }

    // Публичные классы с public no-arg конструктором - KoGenBeansFactory/KoGenComponentsFactory.getInstance()
    // создают их через reflection внутри KoGenScope.getScope().
    class EmptyBeansFactory : KoGenBeansFactory() {
        override fun createBeansList(): Map<Class<*>, KoGenBeans> = emptyMap()
    }

    class EmptyComponentsFactory : KoGenComponentsFactory() {
        override fun createComponentsMap(): Map<Class<*>, KoGenComponents> = emptyMap()
    }
}
