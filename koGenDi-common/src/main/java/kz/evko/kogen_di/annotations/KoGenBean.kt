package kz.evko.kogen_di.annotations

/**
 * Marks a top-level function as a provider for one node in the DI graph - like a `@Provides`
 * method: whatever it returns becomes injectable via the generated `inject()` wherever exactly
 * that return type is requested. Its own parameters are themselves resolved via `inject()`,
 * recursively.
 *
 * Use this instead of `@KoGenComponent` for a type you don't own the constructor of, or one that
 * needs construction logic beyond a plain constructor call.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class KoGenBean(
    /** Whether the same instance is reused for every `inject()` call, or a fresh one is built each time. */
    val singleton: Boolean = false,
)
