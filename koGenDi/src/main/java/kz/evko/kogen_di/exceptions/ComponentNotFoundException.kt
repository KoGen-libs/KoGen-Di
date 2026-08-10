package kz.evko.kogen_di.exceptions

/** Thrown by the generated `inject()` when no `@KoGenComponent`/`@KoGenBean` provides the requested type. */
class ComponentNotFoundException(component: String) : Exception("Component $component not found")

/** Thrown by the generated `koGenViewModel()` when the requested class isn't `@KoGenViewModel`-annotated. */
class ViewModelNotRegisteredException(name: String) : RuntimeException(
    "KoGen DI Error: Could not create an instance of ViewModel '$name'. " +
            "Please ensure the class is annotated with @KoGenViewModel so it can be found by the dependency container."
)

/** Thrown by `inject<Context>()` before the generated `setApplicationContext(context)` has been called for this scope. */
class ContextNotFoundException : Exception("ApplicationContext not set. Do KoGenInjectFactory.setApplicationContext")