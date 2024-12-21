package kz.evko.kogen_di.contentGenerator

class ComponentFactoryGenerator(
    private val packageName: String,
) {
    fun generate(): String {
        return buildString {
            appendLine("package $packageName\n")
            appendLine("import android.content.Context\n")
            appendLine("object KoGenComponentFactory {")
            appendLine("\tval beansFactory: KoGenBeansFactory = KoGenBeansFactory()")
            appendLine("\tval singletons: MutableMap<KoGenComponents, Any> = mutableMapOf()\n")

            appendLine("\tvar applicationContext: Context? = null")
            appendLine("\t\tprivate set\n")

            appendLine("\tfun setApplicationContext(context: Context) {")
            appendLine("\t\tapplicationContext = context")
            appendLine("\t}")

            appendLine("\tinline fun <reified T> inject(): T {")
            appendLine("\t\tval reference = T::class.java")
            appendLine("\t\tval componentName = \"\${reference.`package`?.name}.\${reference.simpleName}\"\n")

            appendLine("\t\tif (reference == Context::class.java) {")
            appendLine("\t\t\tif (applicationContext == null) throw ComponentNotFoundException(\"Context\")")
            appendLine("\t\t\treturn applicationContext as T")
            appendLine("\t\t}\n")

            //appendLine("\t\tval bean = findBeanByType(reference)")
            appendLine("\t\tval bean = beansFactory.findBeanByType(reference)")
            appendLine("\t\tprintln(\"Bean: \$bean\")")
            appendLine("\t\tbean?.let {")
           // appendLine("\t\tfindBeanByType(reference)?.let {")
            appendLine("\t\t\tprintln(\"Type: \$componentName is found as bean \${it.name}\")")
            //appendLine("\t\t\treturn it.bean as T")
            appendLine("\t\t\treturn beansFactory.getBean(it) as T")
            appendLine("\t\t}\n")

            appendLine("\t\tKoGenComponents.entries.firstOrNull { it.componentType.contains(componentName) }?.let {")
            appendLine("\t\t\treturn if (it.singleton) {")
            appendLine("\t\t\t\tif (singletons.containsKey(it)) singletons[it] as T")
            appendLine("\t\t\t\telse {")
            appendLine("\t\t\t\t\tval newComponent = it.getComponentObject()")
            appendLine("\t\t\t\t\tsingletons[it] = newComponent")
            appendLine("\t\t\t\t\tnewComponent as T")
            appendLine("\t\t\t\t}")
            appendLine("\t\t\t} else it.getComponentObject() as T")
            appendLine("\t\t} ?: throw ComponentNotFoundException(componentName)")
            appendLine("\t}")
            appendLine("}\n")

            appendLine("class ComponentNotFoundException(component: String) :")
            appendLine("\tException(\"Component \$component not found\")")
        }
    }
}