package kz.evko.kogen_di.contentGenerator

class InjectFactoryGenerator(
    private val packageName: String,
) {
    fun generateInjectors(includeViewModelInjector: Boolean): String {
        return buildString {
            appendLine("package $packageName\n")

            appendLine("inline fun <reified T> inject(): T {")
            appendLine("\tval reference = T::class.java\n")
            appendLine("\tkz.evko.kogen_di.injector.KoGenScope.getScope(")
            appendLine("\t\tbeansFactoryClass = ${packageName}.KoGenBeansFactoryImpl::class.java,")
            appendLine("\t\tcomponentsFactoryClass = ${packageName}.KoGenComponentsFactoryImpl::class.java,")
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
            appendLine("\t\tbeansFactoryClass = ${packageName}.KoGenBeansFactoryImpl::class.java,")
            appendLine("\t\tcomponentsFactoryClass = ${packageName}.KoGenComponentsFactoryImpl::class.java,")
            appendLine("\t)")
            appendLine("}\n")

            if (includeViewModelInjector) {
                appendLine("@androidx.compose.runtime.Composable")
                appendLine("inline fun <reified T : androidx.lifecycle.ViewModel> koGenViewModel(): T {")
                appendLine("\tval viewModelStoreOwner: androidx.lifecycle.ViewModelStoreOwner = checkNotNull(")
                appendLine("\t\tandroidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner.current")
                appendLine("\t) {")
                appendLine("\t\t\"No ViewModelStoreOwner was provided\"")
                appendLine("\t}")

                appendLine("\treturn androidx.compose.runtime.currentComposer.run {")
                appendLine("\t\tandroidx.compose.runtime.remember {")
                appendLine("\t\t\tval scope = kz.evko.kogen_di.viewModel.KoGenViewModelScope.getInstance(")
                appendLine("\t\t\t\treference = ${packageName}.KoGenViewModelScopeImpl::class.java,")
                appendLine("\t\t\t)")
                appendLine("\t\t\tandroidx.lifecycle.ViewModelProvider(")
                appendLine("\t\t\t\tstore = viewModelStoreOwner.viewModelStore,")
                appendLine("\t\t\t\tfactory = ${packageName}.KoGenViewModelFactory(scope),")
                appendLine("\t\t\t)[T::class.java]")
                appendLine("\t\t}")
                appendLine("\t}")
                appendLine("}\n")

                appendLine("class KoGenViewModelFactory(")
                appendLine("\tprivate val scope: kz.evko.kogen_di.viewModel.KoGenViewModelScope")
                appendLine(") : androidx.lifecycle.ViewModelProvider.Factory {")
                appendLine("\toverride fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {")
                appendLine("\t\treturn scope.getViewModel(modelClass) as T")
                appendLine("\t}")
                appendLine("}")
            }
        }
    }
}