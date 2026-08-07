package kz.evko.kogen_di.injector

import java.util.concurrent.ConcurrentHashMap

abstract class KoGenComponentsFactory {
    private val singleComponents: MutableMap<KoGenComponents, Any> = mutableMapOf()
    private var componentsByType: Map<Class<*>, KoGenComponents> = mapOf()

    fun getComponent(type: Class<*>): Any? {
        if (componentsByType.isEmpty()) {
            componentsByType = createComponentsMap()
        }
        return componentsByType[type]?.let {
            if (it.singleton) {
                singleComponents[it] ?: run {
                    val newComponent = it.getComponentObject()
                    singleComponents[it] = newComponent
                    newComponent
                }
            } else {
                it.getComponentObject()
            }
        }
    }

    abstract fun createComponentsMap(): Map<Class<*>, KoGenComponents>

    companion object {
        private var factories: MutableMap<String, KoGenComponentsFactory> = ConcurrentHashMap()

        fun getInstance(reference: Class<out KoGenComponentsFactory>): KoGenComponentsFactory {
            return factories.getOrPut(reference.name) {
                reference.getConstructor().newInstance()
            }
        }
    }
}

interface KoGenComponents {
    val singleton: Boolean
    fun getComponentObject(): Any
}
