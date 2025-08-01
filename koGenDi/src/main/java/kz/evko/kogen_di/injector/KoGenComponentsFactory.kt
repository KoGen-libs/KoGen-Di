package kz.evko.kogen_di.injector

import java.util.concurrent.ConcurrentHashMap

abstract class KoGenComponentsFactory {
    private val singleComponents: MutableMap<KoGenComponents, Any> = mutableMapOf()

    fun getComponent(name: String): Any? {
        return findComponentByName(name)?.let {
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

    private fun findComponentByName(name: String): KoGenComponents? {
        return componentsList().firstOrNull {
            it.componentType.contains(name)
        }
    }

    abstract fun componentsList(): List<KoGenComponents>

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
    val componentType: Array<out String>
    fun getComponentObject(): Any
}