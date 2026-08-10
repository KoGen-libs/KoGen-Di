package kz.evko.kogen_di.validation

import com.google.devtools.ksp.symbol.KSDeclaration

/**
 * One `@KoGenComponent`/`@KoGenBean`/`@KoGenViewModel` declaration, as [DependencyValidator] needs
 * to see it.
 *
 * @property concreteType This declaration's own fully-qualified name (the class itself for a
 *   component/ViewModel, the function itself for a bean).
 * @property requiredDependencies Fully-qualified types this declaration's constructor/parameters need `inject()` to resolve.
 * @property satisfiableTypes Fully-qualified types this declaration can itself satisfy for some
 *   *other* declaration's [requiredDependencies] - just [concreteType]'s return type for a bean,
 *   or the class's own type plus every supertype (except `Any`) for a component/ViewModel.
 * @property sourceElement Where to attach a KSP compile error if validation fails here.
 */
data class ProviderNode(
    val concreteType: String,
    val requiredDependencies: List<String>,
    val satisfiableTypes: List<String>,
    val sourceElement: KSDeclaration,
)