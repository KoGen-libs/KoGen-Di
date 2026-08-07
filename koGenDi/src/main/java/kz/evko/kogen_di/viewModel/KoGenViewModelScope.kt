package kz.evko.kogen_di.viewModel

import kz.evko.kogen_di.exceptions.ViewModelNotRegisteredException
import java.util.concurrent.ConcurrentHashMap

abstract class KoGenViewModelScope {
    private var viewModelsByType: Map<Class<*>, KoGenViewModels> = mapOf()

    @Suppress("UNCHECKED_CAST")
    fun <T> getViewModel(reference: Class<T>): T? {
        if (viewModelsByType.isEmpty()) {
            viewModelsByType = createViewModelsMap()
        }
        val viewModel = viewModelsByType[reference] ?: throw ViewModelNotRegisteredException(reference.name)
        return viewModel.getComponentObject() as T?
    }

    abstract fun createViewModelsMap(): Map<Class<*>, KoGenViewModels>

    companion object {
        private var scopes: MutableMap<String, KoGenViewModelScope> = ConcurrentHashMap()

        fun getInstance(scopeId: String, reference: Class<out KoGenViewModelScope>): KoGenViewModelScope {
            return scopes.getOrPut(scopeId) {
                reference.getConstructor().newInstance()
            }
        }
    }
}

interface KoGenViewModels {
    fun getComponentObject(): Any
}
