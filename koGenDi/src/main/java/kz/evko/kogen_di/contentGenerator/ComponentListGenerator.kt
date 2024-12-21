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
            appendLine("import $packageName.KoGenInjectFactory.inject\n")
            appendLine("enum class KoGenComponents(")
            appendLine("\tval singleton: Boolean,")
            appendLine("\tvararg val componentType: String,")
            appendLine(") {")

            components.forEach {
                componentItems[it.simpleName.asString()] = it.getName()
                val isSingleton = it.findSingletonParam(KoGenComponent::class)
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

    fun createComponentFactory(): String {
        return buildString {
            appendLine("package $packageName\n")

            appendLine("class KoGenComponentsFactory {")
            appendLine("\tprivate val singleComponents: MutableMap<KoGenComponents, Any> = mutableMapOf()\n")
            appendLine("\tprivate fun findComponentByName(name: String): KoGenComponents? {")
            appendLine("\t\treturn KoGenComponents.entries.firstOrNull {")
            appendLine("\t\t\tit.componentType.contains(name)")
            appendLine("\t\t}")
            appendLine("\t}\n")

            appendLine("\tfun getComponent(name: String): Any? {")
            appendLine("\t\treturn findComponentByName(name)?.let {")
            appendLine("\t\t\tif (it.singleton) {")
            appendLine("\t\t\t\tsingleComponents[it]?.let {")
            appendLine("\t\t\t\t\tit")
            appendLine("\t\t\t\t} ?: run {")
            appendLine("\t\t\t\t\tval newComponent = it.getComponentObject()")
            appendLine("\t\t\t\t\tsingleComponents[it] = newComponent")
            appendLine("\t\t\t\t\tnewComponent")
            appendLine("\t\t\t\t}")
            appendLine("\t\t\t} else {")
            appendLine("\t\t\t\tit.getComponentObject()")
            appendLine("\t\t\t}")
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
}

internal fun KSDeclaration.findSingletonParam(annotationClass: KClass<*>): Boolean {
    val annotation =
        this.annotations.firstOrNull { it.shortName.asString() == annotationClass::class.simpleName.toString() }
    val name = annotation?.arguments?.firstOrNull { it.name?.asString() == "singleton" }
    return name?.value == true
}

internal fun KSDeclaration.getName(): String =
    packageName.asString() + "." + simpleName.asString()