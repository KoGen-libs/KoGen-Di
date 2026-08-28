package kz.evko.kogen_di.viewModel

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import kz.evko.kogen_di.injector.KoGenModuleId
import kotlin.properties.ReadOnlyProperty

/**
 * Obtains a `@KoGenViewModel`-annotated [T], scoped to the current `LocalViewModelStoreOwner` -
 * this library's equivalent of `by viewModels()`, backed by KoGen's own DI graph instead of a
 * hand-written `ViewModelProvider.Factory`. What the generated Compose `koGenViewModel()`
 * one-liner delegates to.
 *
 * @throws IllegalStateException if there's no `LocalViewModelStoreOwner` in scope.
 */
@Composable
inline fun <reified T : ViewModel> KoGenModuleId.koGenViewModel(): T {
    val viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided"
    }
    return remember {
        val scope = KoGenViewModelScope.getInstance(scopeId, viewModelScopeClass)
        ViewModelProvider(
            store = viewModelStoreOwner.viewModelStore,
            factory = KoGenViewModelFactory(scope),
        )[T::class.java]
    }
}

/**
 * Lazily obtains a `@KoGenViewModel`-annotated [T] scoped to [ownerProducer]'s `ViewModelStore` -
 * this library's equivalent of AndroidX's `by viewModels()`, backed by KoGen's own DI graph. What
 * the generated `Fragment` `koGenViewModel()` one-liner delegates to.
 *
 * @param extrasProducer Accepted only to match `by viewModels()`'s call shape - KoGen's own
 *   `ViewModelProvider.Factory` doesn't use `CreationExtras`, so this is ignored.
 * @param ownerProducer The `ViewModelStoreOwner` [T] is scoped to. Defaults to [fragment] itself.
 */
inline fun <reified T : ViewModel> KoGenModuleId.koGenViewModel(
    fragment: Fragment,
    noinline extrasProducer: (() -> CreationExtras)? = null,
    noinline ownerProducer: () -> ViewModelStoreOwner = { fragment },
): ReadOnlyProperty<Fragment, T> {
    val lazyViewModel = koGenViewModelLazy<T>(ownerProducer)
    return ReadOnlyProperty { _, _ -> lazyViewModel.value }
}

/**
 * Lazily obtains a `@KoGenViewModel`-annotated [T] scoped to [ownerProducer]'s `ViewModelStore` -
 * this library's equivalent of AndroidX's `by viewModels()`, backed by KoGen's own DI graph. What
 * the generated `ComponentActivity` `koGenViewModel()` one-liner delegates to.
 *
 * @param extrasProducer Accepted only to match `by viewModels()`'s call shape - KoGen's own
 *   `ViewModelProvider.Factory` doesn't use `CreationExtras`, so this is ignored.
 * @param ownerProducer The `ViewModelStoreOwner` [T] is scoped to. Defaults to [activity] itself.
 */
inline fun <reified T : ViewModel> KoGenModuleId.koGenViewModel(
    activity: ComponentActivity,
    noinline extrasProducer: (() -> CreationExtras)? = null,
    noinline ownerProducer: () -> ViewModelStoreOwner = { activity },
): ReadOnlyProperty<ComponentActivity, T> {
    val lazyViewModel = koGenViewModelLazy<T>(ownerProducer)
    return ReadOnlyProperty { _, _ -> lazyViewModel.value }
}

/** Shared by both [Fragment]/[ComponentActivity] overloads above - builds [T] lazily, on first access. */
@PublishedApi
internal inline fun <reified T : ViewModel> KoGenModuleId.koGenViewModelLazy(
    noinline ownerProducer: () -> ViewModelStoreOwner,
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) {
    val scope = KoGenViewModelScope.getInstance(scopeId, viewModelScopeClass)
    ViewModelProvider(
        owner = ownerProducer(),
        factory = KoGenViewModelFactory(scope),
    )[T::class.java]
}
