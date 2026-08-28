package kz.evko.kogen_di.annotations

/**
 * Requests a specific `@KoGenComponent`/`@KoGenBean` qualifier for one constructor/function
 * parameter that's otherwise auto-wired via a plain `inject()` - the generated equivalent of
 * calling `inject<T>(qualifier = "...")` by hand for just that parameter.
 *
 * Usable on a `@KoGenComponent`/`@KoGenViewModel` primary constructor parameter or a `@KoGenBean`
 * function parameter. Without it, an auto-wired parameter always requests the unqualified (`""`)
 * provider.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class KoGenQualifier(
    val qualifier: String,
)
