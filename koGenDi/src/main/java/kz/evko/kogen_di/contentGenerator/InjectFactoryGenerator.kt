package kz.evko.kogen_di.contentGenerator

class InjectFactoryGenerator(
    private val packageName: String,
) {
    fun generateBeansList(): String {
        return buildString {
            appendLine("package $packageName\n")

            appendLine("inline fun <reified T> inject(): T {")
            appendLine("\tval reference = T::class.java\n")
            appendLine("\tkz.evko.kogen_di.injector.KoGenScope.getScope(")
            appendLine("\t\tbeansFactoryClass = KoGenBeansFactoryImpl::class.java,")
            appendLine("\t\tcomponentsFactoryClass = KoGenComponentsFactoryImpl::class.java,")
            appendLine("\t).run {")
            appendLine("\t\tif (reference == android.content.Context::class.java) {")
            appendLine("\t\t\treturn this.applicationContext as T")
            appendLine("\t\t}")
            appendLine("\t\treturn this.getComponent(T::class.java) as T")
            appendLine("\t}")
            appendLine("}")

            appendLine("fun setApplicationContext(context: android.content.Context) {")
            appendLine("\tkz.evko.kogen_di.injector.KoGenScope.setApplicationContext(")
            appendLine("\t\tcontext = context,")
            appendLine("\t\tbeansFactoryClass = KoGenBeansFactoryImpl::class.java,")
            appendLine("\t\tcomponentsFactoryClass = KoGenComponentsFactoryImpl::class.java,")
            appendLine("\t)")
            appendLine("}")
        }
    }
}