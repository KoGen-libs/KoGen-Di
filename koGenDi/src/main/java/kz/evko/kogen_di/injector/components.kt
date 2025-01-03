package kz.evko.kogen_di.injector

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
        private var instance: KoGenComponentsFactory? = null

        fun getInstance(reference: Class<out KoGenComponentsFactory>): KoGenComponentsFactory {
            return instance ?: synchronized(this) {
                instance = reference.getConstructor().newInstance()
                instance!!
            }
        }
    }
}

interface KoGenComponents {
    val singleton: Boolean
    val componentType: Array<out String>
    fun getComponentObject(): Any
}