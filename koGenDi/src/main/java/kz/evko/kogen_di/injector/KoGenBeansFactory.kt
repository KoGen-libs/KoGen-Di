package kz.evko.kogen_di.injector

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
        private var instance: KoGenBeansFactory? = null

        fun getInstance(reference: Class<out KoGenBeansFactory>): KoGenBeansFactory {
            return instance ?: synchronized(this) {
                instance ?: reference.getConstructor().newInstance().also {
                    instance = it
                }
            }
        }
    }
}

interface KoGenBeans {
    val singleton: Boolean
    fun getComponentObject(): Any
}