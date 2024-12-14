package kz.evko.kogen_di.contentGenerator

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import kz.evko.kogen_di.annotations.KoGenComponent
import kz.evko.kogen_di.kspPackage

class ComponentListGenerator(
    private val logger: KSPLogger,
) {
    fun generateComponentList(components: List<KSClassDeclaration>): String {
        val componentItems: MutableMap<String, String> = mutableMapOf()

        return buildString {
            appendLine("package ${kspPackage()}\n")
            appendLine("enum class KoGenComponent(")
            appendLine("\tval singleton: Boolean,")
            appendLine("\tvararg val componentType: String,")
            appendLine(") {")

            components.forEach {
                componentItems[it.simpleName.asString()] = it.getName()
                val isSingleton = findSingletonParam(it)
                appendLine("\t${it.simpleName.asString()}($isSingleton, ${createNamesLine(it)}),")
            }
            appendLine(";")

            appendLine("\tfun getObject(): Any {")
            appendLine("\t\treturn when (this) {")

            componentItems.forEach {
                appendLine("\t\t\t${it.key} -> ${it.value}()")
            }

            appendLine("\t\t}")
            appendLine("\t}")

            appendLine("}")


        }
    }

    private fun createNamesLine(component: KSClassDeclaration): String {
        val names = mutableListOf(component.getName())

        component.superTypes.forEach {
            names.add(it.resolve().declaration.getName())
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