package kz.evko.kogen_di.annotations

/**
 * Marks a class as a node in the DI graph: its primary constructor's parameters are resolved via
 * the generated `inject()`, recursively, and the class itself - plus every one of its supertypes
 * except `Any` - becomes injectable via `inject()`.
 *
 * Use `@KoGenBean` instead for a type that needs construction logic beyond a plain constructor
 * call.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class KoGenComponent(
    /** Whether the same instance is reused for every `inject()` call, or a fresh one is built each time. */
    val singleton: Boolean = false,
)