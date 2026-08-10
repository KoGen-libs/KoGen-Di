package kz.evko.kogen_di.injector

import kz.evko.kogen_di.exceptions.ComponentNotFoundException
import kz.evko.kogen_di.exceptions.ContextNotFoundException
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-scope DI container the generated `inject()`/`setApplicationContext()` delegate to. Looks a
 * requested type up in [beansFactory] first, then [componentsFactory].
 */
class KoGenScope(
    private var beansFactory: KoGenBeansFactory,
    private var componentsFactory: KoGenComponentsFactory,
) {
    /** Set via [setApplicationContext]; reading it beforehand throws [ContextNotFoundException]. */
    var applicationContext: Any? = null
        get() {
            if (field == null) throw ContextNotFoundException()
            return field
        }
        private set

    /** [reference]'s instance, from [beansFactory] if it provides it, [componentsFactory] otherwise. Throws [ComponentNotFoundException] if neither does. */
    fun getComponent(reference: Class<*>): Any {
        beansFactory.findBeanByType(reference)?.let {
            return beansFactory.getBean(it)
        }

        return componentsFactory.getComponent(reference) ?: throw ComponentNotFoundException(reference.name)
    }

    companion object {
        // One KoGenScope per scopeId (in practice, one per consuming module's generated
        // package) - cached so repeated inject() calls share the same beans/components/context
        // instead of rebuilding the factories on every call.
        private val scopes: MutableMap<String, KoGenScope> = ConcurrentHashMap()

        /** Creates (if needed) the scope for [scopeId] and records [context] as its [applicationContext]. Called once, from the generated `setApplicationContext(context)`. */
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

        /** The scope for [scopeId], creating it - and its [beansFactoryClass]/[componentsFactoryClass] instances - on first use. */
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
