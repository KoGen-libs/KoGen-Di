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

            appendLine("import $packageName.KoGenInjectFactory.inject\n")

            appendLine("enum class KoGenBeans(val singleton: Boolean) {")
            beans.forEach {
                it.returnType?.let { type ->
                    val returnType = type.resolve().declaration
                    val isSingleton = it.findSingletonParam(KoGenBean::class)
                    appendLine(
                        "\t_${returnType.simpleName.asString()}(singleton = $isSingleton),"
                    )
                }
            }
            appendLine("\t;\n")

            appendLine("\tfun getComponentObject(): Any {")
            if (beans.isEmpty()) appendLine("\t\treturn Any()")
            else {
                appendLine("\t\treturn when (this) {")
                beans.forEach {
                    it.returnType?.let { type ->
                        val returnType = type.resolve().declaration
                        appendLine("\t\t\t_${returnType.simpleName.asString()} -> {")
                        if (it.parameters.isEmpty()) {
                            appendLine("\t\t\t\t${it.packageName.asString()}.${it.simpleName.asString()}()")
                        } else {
                            appendLine("\t\t\t\t${it.packageName.asString()}.${it.simpleName.asString()}(")
                            it.parameters.forEach { parameter ->
                                appendLine("\t\t\t\t\t${parameter.name?.asString()} = inject(),")
                            }
                            appendLine("\t\t\t\t)")
                        }
                        appendLine("\t\t\t}")
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

            appendLine("class KoGenBeansFactory {")
            appendLine("\tprivate val singleBeans: MutableMap<KoGenBeans, Any> = mutableMapOf()")
            appendLine("\tprivate var beansList: Map<Class<*>, KoGenBeans> = mapOf()\n")

            appendLine("\tfun findBeanByType(type: Class<*>): KoGenBeans? {")
            appendLine("\t\tif (beansList.isEmpty()) {")
            appendLine("\t\t\tbeansList = createBeansList()")
            appendLine("\t\t}")
            appendLine("\t\treturn beansList[type]")
            appendLine("\t}\n")

            appendLine("\tfun getBean(bean: KoGenBeans): Any {")
            appendLine("\t\treturn if (bean.singleton) {")
            appendLine("\t\t\tsingleBeans[bean]?.let {")
            appendLine("\t\t\t\tit")
            appendLine("\t\t\t} ?: run {")
            appendLine("\t\t\t\tval newBean = bean.getComponentObject()")
            appendLine("\t\t\t\tsingleBeans[bean] = newBean")
            appendLine("\t\t\t\tnewBean")
            appendLine("\t\t\t}")
            appendLine("\t\t} else {")
            appendLine("\t\t\tbean.getComponentObject()")
            appendLine("\t\t}")
            appendLine("\t}\n")

            appendLine("\tprivate fun createBeansList(): Map<Class<*>, KoGenBeans> = mapOf(")
            beans.forEach {
                it.returnType?.let { type ->
                    val returnType = type.resolve().declaration
                    appendLine("\t\t${returnType.packageName.asString()}.${returnType.simpleName.asString()}::class.java to KoGenBeans._${returnType.simpleName.asString()},")
                }
            }
            appendLine("\t)")
            appendLine("}\n")
        }
    }
}