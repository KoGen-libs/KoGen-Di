package kz.evko.kogen_di.injector

import kz.evko.kogen_di.exceptions.ComponentNotFoundException
import kz.evko.kogen_di.exceptions.ContextNotFoundException

class KoGenScope(
    private var beansFactory: KoGenBeansFactory,
    private var componentsFactory: KoGenComponentsFactory,
) {
    var applicationContext: Any? = null
        get() {
            if (field == null) throw ContextNotFoundException()
            return field
        }
        private set

    fun getComponent(reference: Class<*>): Any {
        beansFactory.findBeanByType(reference)?.let {
            return beansFactory.getBean(it)
        }

        val componentName = "${reference.`package`?.name}.${reference.simpleName}"

        componentsFactory.getComponent(componentName)?.let {
            return it
        } ?: throw ComponentNotFoundException(componentName)
    }

    companion object {
        private var instance: KoGenScope? = null

        fun setApplicationContext(
            context: Any,
            beansFactoryClass: Class<out KoGenBeansFactory>,
            componentsFactoryClass: Class<out KoGenComponentsFactory>,
        ) {
            getScope(
                beansFactoryClass = beansFactoryClass,
                componentsFactoryClass = componentsFactoryClass,
            ).applicationContext = context
        }

        fun getScope(
            beansFactoryClass: Class<out KoGenBeansFactory>,
            componentsFactoryClass: Class<out KoGenComponentsFactory>,
        ): KoGenScope {
            return instance ?: synchronized(this) {
                instance ?: KoGenScope(
                    beansFactory = KoGenBeansFactory.getInstance(beansFactoryClass),
                    componentsFactory = KoGenComponentsFactory.getInstance(componentsFactoryClass),
                ).also {
                    instance = it
                }
            }
        }
    }
}
