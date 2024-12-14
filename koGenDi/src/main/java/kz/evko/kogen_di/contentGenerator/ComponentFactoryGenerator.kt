package kz.evko.kogen_di.contentGenerator

class ComponentFactoryGenerator(
    private val packageName: String,
) {
    fun generate(): String {
        return buildString {
            appendLine("package $packageName\n")
            appendLine("object KoGenComponentFactory {")
            appendLine("\tval singletons: MutableMap<KoGenComponent, Any> = mutableMapOf()\n")
            appendLine("\tinline fun <reified T> inject(): T {")
            appendLine("\t\tval componentName = \"\${T::class.java.packageName}.\${T::class.java.simpleName}\"\n")
            appendLine("\t\tval component =")
            appendLine("\t\t\tKoGenComponent.entries.firstOrNull { it.componentType.contains(componentName) }")
            appendLine("\t\tif (component != null) {")
            appendLine("\t\t\treturn if (component.singleton) {")
            appendLine("\t\t\t\tif (singletons.containsKey(component)) singletons[component] as T")
            appendLine("\t\t\t\telse {")
            appendLine("\t\t\t\t\tval newComponent = component.getObject()")
            appendLine("\t\t\t\t\tsingletons[component] = newComponent")
            appendLine("\t\t\t\t\tnewComponent as T")
            appendLine("\t\t\t\t}")
            appendLine("\t\t\t} else component.getObject() as T")
            appendLine("\t\t} else throw ComponentNotFoundException(componentName)")
            appendLine("\t}")
            appendLine("}\n")

            appendLine("class ComponentNotFoundException(component: String) :")
            appendLine("\tException(\"Component \$component not found\")")
        }
    }
}