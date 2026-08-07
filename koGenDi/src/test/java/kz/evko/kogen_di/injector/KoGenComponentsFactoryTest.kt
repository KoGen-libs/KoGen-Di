package kz.evko.kogen_di.injector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class KoGenComponentsFactoryTest {

    private class FakeComponent(override val singleton: Boolean) : KoGenComponents {
        var creations = 0
        override fun getComponentObject(): Any {
            creations++
            return Any()
        }
    }

    private class TestFactory(
        private val map: Map<Class<*>, KoGenComponents>,
    ) : KoGenComponentsFactory() {
        var createCalls = 0
        override fun createComponentsMap(): Map<Class<*>, KoGenComponents> {
            createCalls++
            return map
        }
    }

    @Test
    fun `getComponent returns null for a type nobody provides`() {
        val factory = TestFactory(emptyMap())
        assertNull(factory.getComponent(String::class.java))
    }

    @Test
    fun `getComponent resolves a component registered under an interface it implements`() {
        // это ровно то, как компонент оказывается доступен под несколькими Class-ключами
        // после редизайна на Class-identity lookup - satisfiableClassNames кладёт несколько
        // записей в карту, все на один и тот же экземпляр enum-значения.
        val component = FakeComponent(singleton = false)
        val factory = TestFactory(
            mapOf(
                String::class.java to component,
                CharSequence::class.java to component,
            )
        )

        val resolved = factory.getComponent(CharSequence::class.java)

        assertEquals(1, component.creations)
        assertNotSame(null, resolved)
    }

    @Test
    fun `non-singleton component is created anew on every getComponent call`() {
        val component = FakeComponent(singleton = false)
        val factory = TestFactory(mapOf(String::class.java to component))

        val first = factory.getComponent(String::class.java)
        val second = factory.getComponent(String::class.java)

        assertNotSame(first, second)
        assertEquals(2, component.creations)
    }

    @Test
    fun `singleton component is created once and reused on every subsequent getComponent call`() {
        val component = FakeComponent(singleton = true)
        val factory = TestFactory(mapOf(String::class.java to component))

        val first = factory.getComponent(String::class.java)
        val second = factory.getComponent(String::class.java)

        assertSame(first, second)
        assertEquals(1, component.creations)
    }

    @Test
    fun `createComponentsMap is invoked lazily, only once, across many lookups`() {
        val factory = TestFactory(mapOf(String::class.java to FakeComponent(false)))

        factory.getComponent(String::class.java)
        factory.getComponent(Int::class.java)
        factory.getComponent(String::class.java)

        assertEquals(1, factory.createCalls)
    }

    @Test
    fun `getInstance returns the same factory instance for the same concrete class`() {
        val first = KoGenComponentsFactory.getInstance(EmptyFactoryForInstanceTest::class.java)
        val second = KoGenComponentsFactory.getInstance(EmptyFactoryForInstanceTest::class.java)

        assertSame(first, second)
    }

    class EmptyFactoryForInstanceTest : KoGenComponentsFactory() {
        override fun createComponentsMap(): Map<Class<*>, KoGenComponents> = emptyMap()
    }
}
