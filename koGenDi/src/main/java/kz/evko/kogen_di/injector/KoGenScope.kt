package kz.evko.kogen_di.injector

import kz.evko.kogen_di.exceptions.ComponentNotFoundException
import kz.evko.kogen_di.exceptions.ContextNotFoundException
import java.util.concurrent.ConcurrentHashMap

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
        private val scopes: MutableMap<String, KoGenScope> = ConcurrentHashMap()

        fun setApplicationContext(
            scopeId: String,
            context: Any,
            beansFactoryClass: Class<out KoGenBeansFactory>,
            componentsFactoryClass: Class<out KoGenComponentsFactory>,
        ) {
            val scope = getScope(
                scopeId = scopeId,
                beansFactoryClass = beansFactoryClass,
                componentsFactoryClass = componentsFactoryClass,
            )
            scope.applicationContext = context
        }

        fun getScope(
            scopeId: String,
            beansFactoryClass: Class<out KoGenBeansFactory>,
            componentsFactoryClass: Class<out KoGenComponentsFactory>,
        ): KoGenScope {
            return scopes.getOrPut(scopeId) {
                KoGenScope(
                    beansFactory = KoGenBeansFactory.getInstance(beansFactoryClass),
                    componentsFactory = KoGenComponentsFactory.getInstance(componentsFactoryClass),
                )
            }
        }
    }
}
