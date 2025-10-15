package kz.evko.kogen_di.viewModel

import kz.evko.kogen_di.exceptions.ComponentNotFoundException
import kz.evko.kogen_di.exceptions.ViewModelNotRegisteredException
import java.util.concurrent.ConcurrentHashMap

abstract class KoGenViewModelScope {

    @Suppress("UNCHECKED_CAST")
    fun <T> getViewModel(reference: Class<T>): T? {
        val componentName = "${reference.`package`?.name}.${reference.simpleName}"
        return findComponentByName(componentName)?.getComponentObject() as T?
    }

    private fun findComponentByName(name: String): KoGenViewModels? {
        return componentsList().firstOrNull {
            it.fullName == name
        } ?: throw ViewModelNotRegisteredException(name)
    }

    abstract fun componentsList(): List<KoGenViewModels>

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
    val fullName: String
    fun getComponentObject(): Any
}