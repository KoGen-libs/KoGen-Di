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

            appendLine("enum class KoGenBeans(val type: Class<*>, val bean: Any) {")
            beans.forEach {
                val returnType = it.returnType!!.resolve().declaration
                append("\t_${returnType.simpleName.asString()}(")
                append("${returnType.packageName.asString()}.${returnType.simpleName.asString()}::class.java, ")
                append("${it.packageName.asString()}.${it.simpleName.asString()}()),\n")
            }

            appendLine("}")
        }
    }
}