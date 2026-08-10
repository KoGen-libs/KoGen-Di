package kz.evko.kogen_di.injector

import java.util.concurrent.ConcurrentHashMap

/**
 * Runtime home for every `@KoGenComponent`-provided instance - one generated
 * `KoGenComponentsFactoryImpl` subclass per consuming module, implementing [createComponentsMap]
 * with the component's own type *and* every supertype (except `Any`) it satisfies, each mapped to
 * the same generated enum entry. [KoGenScope.getComponent] falls back to this after
 * `KoGenBeansFactory`.
 */
abstract class KoGenComponentsFactory {
    private val singleComponents: MutableMap<KoGenComponents, Any> = mutableMapOf()
    private var componentsByType: Map<Class<*>, KoGenComponents> = mapOf()

    /** [type]'s instance from [createComponentsMap] - the cached one if `@KoGenComponent` marked it `singleton`, a fresh one otherwise - or `null` if nothing provides [type]. */
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

    /** Every `@KoGenComponent` class's own type and supertypes, mapped to the [KoGenComponents] entry that constructs it. Implemented by the generated `KoGenComponentsFactoryImpl`. */
    abstract fun createComponentsMap(): Map<Class<*>, KoGenComponents>

    companion object {
        private var factories: MutableMap<String, KoGenComponentsFactory> = ConcurrentHashMap()

        /** One instance per factory subclass, cached by class name - effectively a process-wide singleton per generated `KoGenComponentsFactoryImpl`. */
        fun getInstance(reference: Class<out KoGenComponentsFactory>): KoGenComponentsFactory {
            return factories.getOrPut(reference.name) {
                reference.getConstructor().newInstance()
            }
        }
    }
}

/** One `@KoGenComponent` class, as a generated enum entry - [getComponentObject] constructs it, resolving its constructor parameters via `inject()`. */
interface KoGenComponents {
    val singleton: Boolean
    fun getComponentObject(): Any
}
