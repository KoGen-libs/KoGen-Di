package kz.evko.kogen_di.injector

import android.content.Context

/**
 * Resolves [T] from [this] module's DI graph - a `@KoGenComponent`/`@KoGenBean`-provided instance,
 * or the registered application `Context` itself if [T] is `Context`. What the generated `inject()`
 * one-liner delegates to.
 *
 * @param qualifier Matches a `@KoGenComponent`/`@KoGenBean`'s own `qualifier` argument - `""` (the
 *   default) requests the unqualified provider.
 * @throws kz.evko.kogen_di.exceptions.ComponentNotFoundException if nothing provides [T] under
 *   [qualifier].
 * @throws kz.evko.kogen_di.exceptions.ContextNotFoundException if [T] is `Context` and
 *   [setApplicationContext] hasn't been called yet.
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> KoGenModuleId.inject(qualifier: String = ""): T {
    val reference = T::class.java
    val scope = KoGenScope.getScope(scopeId, beansFactoryClass, componentsFactoryClass)
    if (reference == Context::class.java) {
        return scope.applicationContext as T
    }
    return scope.getComponent(reference, qualifier) as T
}

/**
 * Registers [context] as the application `Context` that `inject<Context>()` returns for [this]
 * module. Call this once, before the first `inject()`/`koGenViewModel()` call - typically from
 * `Application.onCreate()`. What the generated `setApplicationContext()` one-liner delegates to.
 */
fun KoGenModuleId.setApplicationContext(context: Context) {
    KoGenScope.setApplicationContext(scopeId, context, beansFactoryClass, componentsFactoryClass)
}
