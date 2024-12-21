package kz.evko.kogen_di.contentGenerator

class InjectFactoryGenerator(
    private val packageName: String,
) {
    fun generateBeansList(): String {
        return buildString {
            appendLine("package $packageName\n")
            appendLine("import android.content.Context\n")
            appendLine("object KoGenInjectFactory {")
            appendLine("\tval beansFactory: KoGenBeansFactory = KoGenBeansFactory()")
            appendLine("\tval componentsFactory: KoGenComponentsFactory = KoGenComponentsFactory()\n")

            appendLine("\tvar applicationContext: Context? = null")
            appendLine("\t\tprivate set\n")

            appendLine("\tfun setApplicationContext(context: Context) {")
            appendLine("\t\tapplicationContext = context")
            appendLine("\t}\n")

            appendLine("\tinline fun <reified T> inject(): T {")
            appendLine("\t\tval reference = T::class.java\n")

            appendLine("\t\tif (reference == Context::class.java) {")
            appendLine("\t\t\tif (applicationContext == null) throw ComponentNotFoundException(\"Context\")")
            appendLine("\t\t\treturn applicationContext as T")
            appendLine("\t\t}\n")

            appendLine("\t\tbeansFactory.findBeanByType(reference)?.let {")
            appendLine("\t\t\treturn beansFactory.getBean(it) as T")
            appendLine("\t\t}\n")

            appendLine("\t\tval componentName = \"\${reference.`package`?.name}.\${reference.simpleName}\"\n")

            appendLine("\t\tcomponentsFactory.getComponent(componentName)?.let {")
            appendLine("\t\t\treturn it as T")
            appendLine("\t\t} ?: throw ComponentNotFoundException(componentName)")
            appendLine("\t}")

            appendLine("}\n")

            appendLine("class ComponentNotFoundException(component: String) :")
            appendLine("\tException(\"Component \$component not found\")")
        }
    }
}