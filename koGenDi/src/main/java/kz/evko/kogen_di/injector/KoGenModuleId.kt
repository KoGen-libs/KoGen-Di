package kz.evko.kogen_di.injector

import kz.evko.kogen_di.viewModel.KoGenViewModelScope

/**
 * Per-consuming-module identity - the generated `KoGenInjectors.kt` builds exactly one of these,
 * as a `@PublishedApi internal val`, and every generated `inject()`/`setApplicationContext()`/
 * `koGenViewModel()` entry point is just a one-line delegate through it to the real implementation
 * living in this library (see the extension functions in this package and in
 * `kz.evko.kogen_di.viewModel`).
 *
 * @property viewModelScopeClass Generated alongside [beansFactoryClass]/[componentsFactoryClass]
 *   whenever the consuming module has any `@KoGenComponent`/`@KoGenBean`/`@KoGenViewModel` at all,
 *   regardless of whether ViewModel support is actually enabled for that module.
 */
data class KoGenModuleId(
    val scopeId: String,
    val beansFactoryClass: Class<out KoGenBeansFactory>,
    val componentsFactoryClass: Class<out KoGenComponentsFactory>,
    val viewModelScopeClass: Class<out KoGenViewModelScope>,
)
