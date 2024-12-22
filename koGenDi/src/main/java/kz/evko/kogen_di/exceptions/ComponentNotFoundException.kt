package kz.evko.kogen_di.exceptions

class ComponentNotFoundException(component: String) : Exception("Component $component not found")

class ContextNotFoundException : Exception("ApplicationContext not set. Do KoGenInjectFactory.setApplicationContext")