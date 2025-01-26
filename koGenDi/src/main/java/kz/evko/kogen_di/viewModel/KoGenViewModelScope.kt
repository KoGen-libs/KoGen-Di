package kz.evko.kogen_di.viewModel

abstract class KoGenViewModelScope {

    @Suppress("UNCHECKED_CAST")
    fun <T> getViewModel(reference: Class<T>): T? {
        val componentName = "${reference.`package`?.name}.${reference.simpleName}"
        return findComponentByName(componentName)?.getComponentObject() as T?
    }

    private fun findComponentByName(name: String): KoGenViewModels? {
        return componentsList().firstOrNull {
            it.fullName == name
        }
    }

    abstract fun componentsList(): List<KoGenViewModels>

    companion object {
        private var instance: KoGenViewModelScope? = null

        fun getInstance(reference: Class<out KoGenViewModelScope>): KoGenViewModelScope {
            return instance ?: synchronized(this) {
                instance ?: reference.getConstructor().newInstance().also {
                    instance = it
                }
            }
        }
    }
}

interface KoGenViewModels {
    val fullName: String
    fun getComponentObject(): Any
}