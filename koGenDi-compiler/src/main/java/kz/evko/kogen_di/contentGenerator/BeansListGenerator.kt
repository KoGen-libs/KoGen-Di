package kz.evko.kogen_di.contentGenerator

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import kz.evko.kogen_di.annotations.KoGenBean

class BeansListGenerator(
    private val logger: KSPLogger,
    private val packageName: String,
) {
    fun generateBeansList(beans: List<KSFunctionDeclaration>): String {
        return buildString {
            appendLine("package $packageName\n")

            appendLine("enum class KoGenBeansImpl(override val singleton: Boolean): kz.evko.kogen_di.injector.KoGenBeans {")
            beans.forEach {
                it.returnType?.let { type ->
                    val returnType = type.resolve().declaration
                    val isSingleton = it.findSingletonParam(KoGenBean::class)
                    appendLine(
                        "\t${returnType.createComponentNames()}(singleton = $isSingleton),"
                    )
                }
            }
            appendLine("\t;\n")

            appendLine("\toverride fun getComponentObject(): Any {")
            if (beans.isEmpty()) appendLine("\t\treturn Any()")
            else {
                appendLine("\t\treturn when (this) {")
                beans.forEach {
                    it.returnType?.let { type ->
                        val returnType = type.resolve().declaration
                        appendLine("\t\t\t${returnType.createComponentNames()} -> {")
                        if (it.parameters.isEmpty()) {
                            appendLine("\t\t\t\t${it.getName()}()")
                        } else {
                            appendLine("\t\t\t\t${it.getName()}(")
                            it.parameters.forEach { parameter ->
                                appendLine("\t\t\t\t\t${parameter.name?.asString()} = inject(),")
                            }
                            appendLine("\t\t\t\t)")
                        }
                        appendLine("\t\t\t}\n")
                    }
                }
                appendLine("\t\t}")
            }

            appendLine("\t}")
            appendLine("}\n")
        }
    }

    fun generateBeansFactory(beans: List<KSFunctionDeclaration>): String {
        return buildString {
            appendLine("package $packageName\n")

            appendLine("class KoGenBeansFactoryImpl : kz.evko.kogen_di.injector.KoGenBeansFactory() {")

            appendLine("\toverride fun createBeansList(): Map<Class<*>, kz.evko.kogen_di.injector.KoGenBeans> = mapOf(")
            beans.forEach {
                it.returnType?.let { type ->
                    val returnType = type.resolve().declaration
                    appendLine("\t\t${returnType.getName()}::class.java to ${packageName}.KoGenBeansImpl.${returnType.createComponentNames()},")
                }
            }
            appendLine("\t)")
            appendLine("}\n")
        }
    }
}