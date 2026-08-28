package kz.evko.kogen_di.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * `ViewModelProvider.Factory` that resolves a requested ViewModel through [scope]'s DI graph
 * instead of constructing it directly - what `koGenViewModel()` uses under the hood.
 */
class KoGenViewModelFactory(
    private val scope: KoGenViewModelScope,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return scope.getViewModel(modelClass) as T
    }
}
