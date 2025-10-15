package kz.evko.kogen_di.exceptions

class ComponentNotFoundException(component: String) : Exception("Component $component not found")

class ViewModelNotRegisteredException(name: String) : RuntimeException(
    "KoGen DI Error: Could not create an instance of ViewModel '$name'. " +
            "Please ensure the class is annotated with @KoGenViewModel so it can be found by the dependency container."
)

class ContextNotFoundException : Exception("ApplicationContext not set. Do KoGenInjectFactory.setApplicationContext")