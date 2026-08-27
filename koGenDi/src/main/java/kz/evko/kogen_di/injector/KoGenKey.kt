package kz.evko.kogen_di.injector

/**
 * One (type, qualifier) pair a `@KoGenComponent`/`@KoGenBean` is registered under -
 * [KoGenComponentsFactory]/[KoGenBeansFactory] key their maps on this instead of on [type] alone,
 * so the same type can be provided more than once under different qualifiers, Koin-`named()`-style.
 *
 * @property qualifier `""` means "no qualifier" - what a plain `inject<T>()` requests.
 */
data class KoGenKey(
    val type: Class<*>,
    val qualifier: String = "",
)
