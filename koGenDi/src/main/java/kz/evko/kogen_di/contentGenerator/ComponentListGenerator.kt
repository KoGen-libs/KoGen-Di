package kz.evko.kogen_di.contentGenerator

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import kz.evko.kogen_di.annotations.KoGenComponent
import kotlin.reflect.KClass

class ComponentListGenerator(
    private val logger: KSPLogger,
    private val packageName: String,
) {
    fun generateComponentList(components: List<KSClassDeclaration>): String {
        val componentItems: MutableMap<String, String> = mutableMapOf()

        return buildString {
            appendLine("package $packageName\n")

            appendLine("enum class KoGenComponentsImpl(")
            appendLine("\toverride val singleton: Boolean,")
            appendLine("\toverride vararg val componentType: String,")
            appendLine("): kz.evko.kogen_di.injector.KoGenComponents {")

            components.forEach {
                val name = it.createComponentNames()
                componentItems[name] = it.getName()
                val isSingleton = it.findSingletonParam(KoGenComponent::class)
                appendLine("\t$name($isSingleton, ${createNamesLine(it)}),")
            }
            appendLine("\t;")

            appendLine("\toverride fun getComponentObject(): Any {")
            if (components.isNotEmpty()) {
                appendLine("\t\treturn when (this) {")

                components.forEach {
                    val values = it.primaryConstructor?.parameters.orEmpty()
                    val name = it.createComponentNames()
                    componentItems[name]?.let { fullName ->
                        if (values.isEmpty()) {
                            appendLine("\t\t\t$name -> $fullName()")
                        } else {
                            appendLine("\t\t\t$name -> $fullName(")
                            values.forEach { value ->
                                appendLine("\t\t\t\t${value.name?.asString()} = inject(),")
                            }
                            appendLine("\t\t\t)\n")
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

    fun createComponentFactory(): String {
        return buildString {
            appendLine("package $packageName\n")

            appendLine("class KoGenComponentsFactoryImpl : kz.evko.kogen_di.injector.KoGenComponentsFactory() {")

            appendLine("\toverride fun componentsList(): List<kz.evko.kogen_di.injector.KoGenComponents> =")
            appendLine("\t\tKoGenComponentsImpl.entries")
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
}

internal fun KSDeclaration.findSingletonParam(annotationClass: KClass<*>): Boolean {
    val annotation =
        this.annotations.firstOrNull { it.shortName.asString() == annotationClass::class.simpleName.toString() }
    val name = annotation?.arguments?.firstOrNull { it.name?.asString() == "singleton" }
    return name?.value == true
}

internal fun KSDeclaration.getName(): String =
    packageName.asString() + "." + simpleName.asString()

internal fun KSDeclaration.createComponentNames(): String =
    getName().replace(".", "_")