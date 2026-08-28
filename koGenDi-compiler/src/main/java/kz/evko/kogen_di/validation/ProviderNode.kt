package kz.evko.kogen_di.validation

import com.google.devtools.ksp.symbol.KSDeclaration

/**
 * One `@KoGenComponent`/`@KoGenBean`/`@KoGenViewModel` declaration, as [DependencyValidator] needs
 * to see it.
 *
 * @property concreteType This declaration's own fully-qualified name (the class itself for a
 *   component/ViewModel, the function itself for a bean).
 * @property requiredDependencies What this declaration's constructor/function parameters need
 *   `inject()` to resolve - each paired with its own `@KoGenQualifier`, if any.
 * @property satisfiableTypes Fully-qualified types this declaration can itself satisfy for some
 *   *other* declaration's [requiredDependencies] - just [concreteType]'s return type for a bean,
 *   or the class's own type plus every supertype (except `Any`) for a component/ViewModel.
 * @property sourceElement Where to attach a KSP compile error if validation fails here.
 * @property qualifier This declaration's `qualifier` argument (`""` if unset) - what a
 *   [requiredDependencies] entry's own [RequiredDependency.qualifier] must match for this node to
 *   satisfy it.
 */
data class ProviderNode(
    val concreteType: String,
    val requiredDependencies: List<RequiredDependency>,
    val satisfiableTypes: List<String>,
    val sourceElement: KSDeclaration,
    val qualifier: String = "",
)

/**
 * One constructor/function parameter [DependencyValidator] needs to resolve - a fully-qualified
 * [type], plus the [qualifier] its `@KoGenQualifier` requests (`""` - the default - meaning "no
 * qualifier requested", i.e. resolved the same as a plain `inject<T>()`).
 */
data class RequiredDependency(
    val type: String,
    val qualifier: String = "",
)
