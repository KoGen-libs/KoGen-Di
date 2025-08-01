package kz.evko.kogen_di.injector

import java.util.concurrent.ConcurrentHashMap

abstract class KoGenBeansFactory {
    private val singleBeans: MutableMap<KoGenBeans, Any> = mutableMapOf()
    private var beansList: Map<Class<*>, KoGenBeans> = mapOf()

    fun findBeanByType(type: Class<*>): KoGenBeans? {
        if (beansList.isEmpty()) {
            beansList = createBeansList()
        }
        return beansList[type]
    }

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

    abstract fun createBeansList(): Map<Class<*>, KoGenBeans>

    companion object {
        private var factories: MutableMap<String, KoGenBeansFactory> = ConcurrentHashMap()

        fun getInstance(reference: Class<out KoGenBeansFactory>): KoGenBeansFactory {
            return factories.getOrPut(reference.name) {
                reference.getConstructor().newInstance()
            }
        }
    }
}

interface KoGenBeans {
    val singleton: Boolean
    fun getComponentObject(): Any
}