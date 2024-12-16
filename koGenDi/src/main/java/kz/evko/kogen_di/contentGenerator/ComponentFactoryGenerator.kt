package kz.evko.kogen_di.contentGenerator

class ComponentFactoryGenerator(
    private val packageName: String,
) {
    fun generate(): String {
        return buildString {
            appendLine("package $packageName\n")
            appendLine("object KoGenComponentFactory {")
            appendLine("\tval singletons: MutableMap<KoGenComponents, Any> = mutableMapOf()\n")
            appendLine("\tinline fun <reified T> inject(): T {")
            appendLine("\t\tval reference = T::class.java")
            appendLine("\t\tval componentName = \"\${reference.packageName}.\${reference.simpleName}\"\n")

            appendLine("\t\tKoGenBeans.entries.firstOrNull { it.type == reference }?.let {")
            appendLine("\t\t\treturn it.bean as T")
            appendLine("\t\t}\n")

            appendLine("\t\tKoGenComponents.entries.firstOrNull { it.componentType.contains(componentName) }?.let {")
            appendLine("\t\t\treturn if (it.singleton) {")
            appendLine("\t\t\t\tif (singletons.containsKey(it)) singletons[it] as T")
            appendLine("\t\t\t\telse {")
            appendLine("\t\t\t\t\tval newComponent = it.getObject()")
            appendLine("\t\t\t\t\tsingletons[it] = newComponent")
            appendLine("\t\t\t\t\tnewComponent as T")
            appendLine("\t\t\t\t}")
            appendLine("\t\t\t} else it.getObject() as T")
            appendLine("\t\t} ?: throw ComponentNotFoundException(componentName)")
            appendLine("\t}")
            appendLine("}\n")

            appendLine("class ComponentNotFoundException(component: String) :")
            appendLine("\tException(\"Component \$component not found\")")
        }
    }
}