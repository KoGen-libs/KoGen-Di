package kz.evko.kogen_di.contentGenerator

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import kz.evko.kogen_di.annotations.KoGenComponent

class ComponentListGenerator(
    private val logger: KSPLogger,
    private val packageName: String,
) {
    fun generateComponentList(components: List<KSClassDeclaration>): String {
        val componentItems: MutableMap<String, String> = mutableMapOf()

        return buildString {
            appendLine("package $packageName\n")
            appendLine("import $packageName.KoGenComponentFactory.inject\n")
            appendLine("enum class KoGenComponents(")
            appendLine("\tval singleton: Boolean,")
            appendLine("\tvararg val componentType: String,")
            appendLine(") {")

            components.forEach {
                componentItems[it.simpleName.asString()] = it.getName()
                val isSingleton = findSingletonParam(it)
                appendLine("\t_${it.simpleName.asString()}($isSingleton, ${createNamesLine(it)}),")
            }
            appendLine("\t;")

            appendLine("\tfun getComponentObject(): Any {")
            appendLine("\t\treturn when (this) {")

            components.forEach {
                val values = it.primaryConstructor?.parameters.orEmpty()
                val name = it.simpleName.asString()
                componentItems[name]?.let { fullName ->
                    if (values.isEmpty()) {
                        appendLine("\t\t\t_$name -> $fullName()")
                    } else {
                        appendLine("\t\t\t_$name -> $fullName(")
                        values.forEach { value ->
                            appendLine("\t\t\t\t${value.name?.asString()} = inject(),")
                        }
                        appendLine("\t\t\t)")
                    }
                }
            }

            appendLine("\t\t}")
            appendLine("\t}")

            appendLine("}")


        }
    }

    private fun createNamesLine(component: KSClassDeclaration): String {
        val names = mutableListOf(component.getName())

        component.superTypes.forEach {
            val name = it.resolve().declaration.getName()
            if (name != "kotlin.Any") {
                names.add(name)
            }
        }

        return names.joinToString(", ") {
            "\"$it\""
        }
    }

    private fun findSingletonParam(component: KSClassDeclaration): Boolean {
        val annotation =
            component.annotations.first { it.shortName.asString() == KoGenComponent::class.simpleName.toString() }
        val name = annotation.arguments.first { it.name?.asString() == "singleton" }
        return name.value == true
    }
}

internal fun KSDeclaration.getName(): String =
    packageName.asString() + "." + simpleName.asString()