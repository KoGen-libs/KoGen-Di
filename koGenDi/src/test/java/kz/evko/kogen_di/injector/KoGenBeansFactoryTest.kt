package kz.evko.kogen_di.injector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class KoGenBeansFactoryTest {

    private class FakeBean(override val singleton: Boolean) : KoGenBeans {
        var creations = 0
        override fun getComponentObject(): Any {
            creations++
            return Any()
        }
    }

    private class TestFactory(
        private val map: Map<Class<*>, KoGenBeans>,
    ) : KoGenBeansFactory() {
        var createCalls = 0
        override fun createBeansList(): Map<Class<*>, KoGenBeans> {
            createCalls++
            return map
        }
    }

    @Test
    fun `findBeanByType returns null for a type nobody provides`() {
        val factory = TestFactory(emptyMap())
        assertNull(factory.findBeanByType(String::class.java))
    }

    @Test
    fun `findBeanByType finds a registered bean by exact class`() {
        val bean = FakeBean(singleton = false)
        val factory = TestFactory(mapOf(String::class.java to bean))
        assertSame(bean, factory.findBeanByType(String::class.java))
    }

    @Test
    fun `non-singleton bean is created anew on every getBean call`() {
        val bean = FakeBean(singleton = false)
        val factory = TestFactory(mapOf(String::class.java to bean))

        val first = factory.getBean(bean)
        val second = factory.getBean(bean)

        assertNotSame(first, second)
        assertEquals(2, bean.creations)
    }

    @Test
    fun `singleton bean is created once and reused on every subsequent getBean call`() {
        val bean = FakeBean(singleton = true)
        val factory = TestFactory(mapOf(String::class.java to bean))

        val first = factory.getBean(bean)
        val second = factory.getBean(bean)
        val third = factory.getBean(bean)

        assertSame(first, second)
        assertSame(first, third)
        assertEquals(1, bean.creations)
    }

    @Test
    fun `createBeansList is invoked lazily, only once, across many lookups`() {
        val factory = TestFactory(mapOf(String::class.java to FakeBean(false)))

        factory.findBeanByType(String::class.java)
        factory.findBeanByType(Int::class.java)
        factory.findBeanByType(String::class.java)

        assertEquals(1, factory.createCalls)
    }

    @Test
    fun `getInstance returns the same factory instance for the same concrete class`() {
        val first = KoGenBeansFactory.getInstance(EmptyFactoryForInstanceTest::class.java)
        val second = KoGenBeansFactory.getInstance(EmptyFactoryForInstanceTest::class.java)

        assertSame(first, second)
    }

    // Публичный класс с public no-arg конструктором - getInstance создаёт его через reflection
    // (Class.getConstructor().newInstance()), так что не может быть private/anonymous.
    class EmptyFactoryForInstanceTest : KoGenBeansFactory() {
        override fun createBeansList(): Map<Class<*>, KoGenBeans> = emptyMap()
    }
}
