package kz.evko.kogen_di.contentGenerator

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration

class ViewModelListGenerator(
    private val logger: KSPLogger,
    private val packageName: String,
) {
    fun generateViewModelList(viewModels: List<KSClassDeclaration>): String {
        return buildString {
            appendLine("package $packageName\n")

            appendLine("enum class KoGenViewModelsImpl(override val fullName: String): kz.evko.kogen_di.viewModel.KoGenViewModels {")
            viewModels.forEach {
                appendLine("\t${it.createComponentNames()}(\"${it.getName()}\"),")
            }
            appendLine("\t;\n")

            appendLine("\toverride fun getComponentObject(): Any {")
            if (viewModels.isNotEmpty()) {
                appendLine("\t\treturn when (this) {")

                viewModels.forEach {
                    it.primaryConstructor?.parameters.orEmpty().run {
                        if (isEmpty()) {
                            appendLine("\t\t\t${it.createComponentNames()} -> ${it.getName()}()")
                        } else {
                            appendLine("\t\t\t${it.createComponentNames()} -> ${it.getName()}(")
                            forEach { param ->
                                appendLine("\t\t\t\t${param.name?.asString()} = inject(),")
                            }
                            appendLine("\t\t\t)")
                        }
                    }
                }
                appendLine("\t\t}")
            } else {
                appendLine("\t\treturn Any()")
            }
            appendLine("\t}")

            appendLine("}")
        }
    }

    fun generateViewModelFactory(): String {
        return buildString {
            appendLine("package $packageName\n")

            appendLine("class KoGenViewModelScopeImpl : kz.evko.kogen_di.viewModel.KoGenViewModelScope() {")

            appendLine("\toverride fun componentsList(): List<kz.evko.kogen_di.viewModel.KoGenViewModels> = \n\t\tKoGenViewModelsImpl.entries")

            appendLine("}")
        }
    }
}