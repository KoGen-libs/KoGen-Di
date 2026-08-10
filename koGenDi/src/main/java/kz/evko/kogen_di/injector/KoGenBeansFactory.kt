package kz.evko.kogen_di.injector

import java.util.concurrent.ConcurrentHashMap

/**
 * Runtime home for every `@KoGenBean`-provided instance - one generated `KoGenBeansFactoryImpl`
 * subclass per consuming module, implementing [createBeansList] with everything that module's KSP
 * round found annotated. [KoGenScope.getComponent] tries this before falling back to
 * `KoGenComponentsFactory`.
 */
abstract class KoGenBeansFactory {
    private val singleBeans: MutableMap<KoGenBeans, Any> = mutableMapOf()
    private var beansList: Map<Class<*>, KoGenBeans> = mapOf()

    /** [createBeansList]'s entry for [type], if any - built lazily on first lookup rather than eagerly. */
    fun findBeanByType(type: Class<*>): KoGenBeans? {
        if (beansList.isEmpty()) {
            beansList = createBeansList()
        }
        return beansList[type]
    }

    /** [bean]'s instance - the cached one if `@KoGenBean` marked it `singleton`, a fresh one otherwise. */
    fun getBean(bean: KoGenBeans): Any {
        return if (bean.singleton) {
            singleBeans[bean] ?: run {
                val newBean = bean.getComponentObject()
                singleBeans[bean] = newBean
                newBean
            }
        } else {
            bean.getComponentObject()
        }
    }

    /** Every `@KoGenBean` function's return type, mapped to the [KoGenBeans] entry that calls it. Implemented by the generated `KoGenBeansFactoryImpl`. */
    abstract fun createBeansList(): Map<Class<*>, KoGenBeans>

    companion object {
        private var factories: MutableMap<String, KoGenBeansFactory> = ConcurrentHashMap()

        /** One instance per factory subclass, cached by class name - effectively a process-wide singleton per generated `KoGenBeansFactoryImpl`. */
        fun getInstance(reference: Class<out KoGenBeansFactory>): KoGenBeansFactory {
            return factories.getOrPut(reference.name) {
                reference.getConstructor().newInstance()
            }
        }
    }
}

/** One `@KoGenBean` function, as a generated enum entry - [getComponentObject] calls that function, resolving its own parameters via `inject()`. */
interface KoGenBeans {
    val singleton: Boolean
    fun getComponentObject(): Any
}