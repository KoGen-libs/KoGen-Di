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
    private var componentsByKey: Map<KoGenKey, KoGenComponents> = mapOf()

    /** [type]/[qualifier]'s instance from [createComponentsMap] - the cached one if `@KoGenComponent` marked it `singleton`, a fresh one otherwise - or `null` if nothing provides that (type, qualifier) pair. */
    fun getComponent(type: Class<*>, qualifier: String = ""): Any? {
        if (componentsByKey.isEmpty()) {
            componentsByKey = createComponentsMap()
        }
        return componentsByKey[KoGenKey(type, qualifier)]?.let {
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

    /** Every `@KoGenComponent` class's own type and supertypes - each paired with its declaration's `qualifier` - mapped to the [KoGenComponents] entry that constructs it. Implemented by the generated `KoGenComponentsFactoryImpl`. */
    abstract fun createComponentsMap(): Map<KoGenKey, KoGenComponents>

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
