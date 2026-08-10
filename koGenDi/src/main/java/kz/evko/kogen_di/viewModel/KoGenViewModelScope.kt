package kz.evko.kogen_di.viewModel

import kz.evko.kogen_di.exceptions.ViewModelNotRegisteredException
import java.util.concurrent.ConcurrentHashMap

/**
 * Runtime home for every `@KoGenViewModel` instance - one generated `KoGenViewModelScopeImpl`
 * subclass per consuming module, implementing [createViewModelsMap]. Deliberately separate from
 * `KoGenComponentsFactory`/`KoGenBeansFactory`: a ViewModel is obtained through the generated
 * `koGenViewModel()` (backed by a real `ViewModelProvider`, so it gets Android's normal ViewModel
 * lifecycle), not through `inject()`.
 */
abstract class KoGenViewModelScope {
    private var viewModelsByType: Map<Class<*>, KoGenViewModels> = mapOf()

    /** [reference]'s instance, built lazily from [createViewModelsMap] on first call. Throws [ViewModelNotRegisteredException] if [reference] isn't `@KoGenViewModel`-annotated. */
    @Suppress("UNCHECKED_CAST")
    fun <T> getViewModel(reference: Class<T>): T? {
        if (viewModelsByType.isEmpty()) {
            viewModelsByType = createViewModelsMap()
        }
        val viewModel = viewModelsByType[reference] ?: throw ViewModelNotRegisteredException(reference.name)
        return viewModel.getComponentObject() as T?
    }

    /** Every `@KoGenViewModel` class, mapped to the [KoGenViewModels] entry that constructs it. Implemented by the generated `KoGenViewModelScopeImpl`. */
    abstract fun createViewModelsMap(): Map<Class<*>, KoGenViewModels>

    companion object {
        // One KoGenViewModelScope per scopeId, cached - mirrors KoGenScope.getScope, kept as a
        // separate map/companion because it's a wholly separate scope kind (see the class doc).
        private var scopes: MutableMap<String, KoGenViewModelScope> = ConcurrentHashMap()

        /** The scope for [scopeId], creating (and caching by [scopeId]) a [reference] instance on first use. */
        fun getInstance(scopeId: String, reference: Class<out KoGenViewModelScope>): KoGenViewModelScope {
            return scopes.getOrPut(scopeId) {
                reference.getConstructor().newInstance()
            }
        }
    }
}

/** One `@KoGenViewModel` class, as a generated enum entry - [getComponentObject] constructs it, resolving its constructor parameters via `inject()`. */
interface KoGenViewModels {
    fun getComponentObject(): Any
}
