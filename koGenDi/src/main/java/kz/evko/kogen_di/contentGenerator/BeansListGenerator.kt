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

            appendLine("enum class KoGenBeans(val bean: Any) {")
            beans.forEach {
                it.returnType?.let { type ->
                    val returnType = type.resolve().declaration
                    appendLine("\t_${returnType.simpleName.asString()}(")
                    if (it.parameters.isEmpty()) {
                        appendLine("\t\tbean = ${it.packageName.asString()}.${it.simpleName.asString()}(),")
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

            appendLine("}\n")

            appendLine("fun findBeanByType(type: Class<*>): KoGenBeans? {")
            appendLine("\treturn when (type) {")
            beans.forEach {
                it.returnType?.let { type ->
                    val returnType = type.resolve().declaration
                    appendLine("\t\t${returnType.packageName.asString()}.${returnType.simpleName.asString()}::class.java -> KoGenBeans._${returnType.simpleName.asString()}")
                }
            }
            appendLine("\t\telse -> null")

            appendLine("\t}")
            appendLine("}")
        }
    }
}