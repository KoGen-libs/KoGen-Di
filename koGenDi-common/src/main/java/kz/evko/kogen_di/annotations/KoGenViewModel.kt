package kz.evko.kogen_di.annotations

/**
 * Marks a `ViewModel` subclass as constructible through KoGen DI: its primary constructor's
 * parameters are resolved via the generated `inject()`. The ViewModel itself is deliberately
 * *not* obtained via `inject()` though - it's kept in a separate scope from
 * `@KoGenComponent`/`@KoGenBean` and obtained through the generated `koGenViewModel()` instead,
 * so it gets Android's normal ViewModel lifecycle (survives configuration changes, is scoped to
 * its owner) rather than KoGen's plain instance/singleton semantics.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class KoGenViewModel()
