package kz.evko.kogen_di.contentGenerator

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

class BeansListGenerator(
    private val logger: KSPLogger,
    private val packageName: String,
) {
    fun generateBeansList(beans: List<KSFunctionDeclaration>): String {
        return buildString {
            appendLine("package $packageName\n")

            appendLine("import $packageName.KoGenComponentFactory.inject\n")

            appendLine("enum class KoGenBeans(val type: Class<*>, val bean: Any) {")
            beans.forEach {
                it.returnType?.let { type ->
                    val returnType = type.resolve().declaration
                    appendLine("\t_${returnType.simpleName.asString()}(")
                    appendLine("\t\ttype = ${returnType.packageName.asString()}.${returnType.simpleName.asString()}::class.java,")
                    if (it.parameters.isEmpty()) {
                        appendLine("\t\tbean = ${it.packageName.asString()}.${it.simpleName.asString()}()),")
                    } else {
                        appendLine("\t\tbean = ${it.packageName.asString()}.${it.simpleName.asString()}(")
                        it.parameters.forEach { parameter ->
                            appendLine("\t\t\t${parameter.name?.asString()} = inject(),")
                        }
                        appendLine("\t\t),")
                    }
                    appendLine("\t),")
                }
            }

            appendLine("}")
        }
    }
}