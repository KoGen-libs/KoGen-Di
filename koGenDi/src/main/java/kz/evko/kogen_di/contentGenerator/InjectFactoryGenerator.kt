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
            appendLine("}\n")

            appendLine("fun setApplicationContext(context: android.content.Context) {")
            appendLine("\tkz.evko.kogen_di.injector.KoGenScope.setApplicationContext(")
            appendLine("\t\tcontext = context,")
            appendLine("\t\tbeansFactoryClass = KoGenBeansFactoryImpl::class.java,")
            appendLine("\t\tcomponentsFactoryClass = KoGenComponentsFactoryImpl::class.java,")
            appendLine("\t)")
            appendLine("}\n")

            appendLine("inline fun <reified T : androidx.lifecycle.ViewModel> koGenViewModel(): T {")
            appendLine("\treturn androidx.lifecycle.ViewModelProvider(")
            appendLine("\t\tstore = androidx.lifecycle.ViewModelStore(),")
            appendLine("\t\tfactory = KoGenViewModelFactory(),")
            appendLine("\t)[T::class.java]")
            appendLine("}\n")

            appendLine("class KoGenViewModelFactory : androidx.lifecycle.ViewModelProvider.Factory {")
            appendLine("\toverride fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {")
            appendLine("\t\treturn kz.evko.kogen_di.viewModel.KoGenViewModelScope.getInstance(")
            appendLine("\t\t\tKoGenViewModelScopeImpl::class.java")
            appendLine("\t\t).getViewModel(modelClass) as T")
            appendLine("\t}")
            appendLine("}")

        }
    }
}