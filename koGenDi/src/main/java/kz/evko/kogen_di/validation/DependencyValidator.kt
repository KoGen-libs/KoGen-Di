package kz.evko.kogen_di.validation

import com.google.devtools.ksp.processing.KSPLogger

class DependencyValidator(
    allProviders: List<ProviderNode>,
    private val logger: KSPLogger
) {
    private val providerMap: Map<String, ProviderNode>
    private val externalProviders = setOf("android.content.Context")
    private var validationFailed = false

    private val ignoredAmbiguityTypes = setOf(
        "androidx.lifecycle.ViewModel",
        "java.io.Serializable",
        "kotlin.Any"
    )

    init {
        val mutableProviderMap = mutableMapOf<String, ProviderNode>()

        allProviders.forEach { node ->
            node.satisfiableTypes.forEach { type ->
                if (type in ignoredAmbiguityTypes) return@forEach

                if (mutableProviderMap.containsKey(type)) {
                    val existingNode = mutableProviderMap[type]
                    logger.error(
                        "Ambiguous dependency: Type '$type' is provided by both '${existingNode?.concreteType}' and '${node.concreteType}'.",
                        node.sourceElement
                    )
                    logger.warn("    - First provider is at", existingNode?.sourceElement)
                    validationFailed = true
                } else {
                    mutableProviderMap[type] = node
                }
            }
        }
        providerMap = mutableProviderMap
    }

    fun validate() {
        if (validationFailed) return

        providerMap.values.distinct().forEach { node ->
            node.requiredDependencies.forEach { dependency ->
                if (dependency !in providerMap && dependency !in externalProviders) {
                    logger.error(
                        "Missing dependency: '$dependency' is required by '${node.concreteType}' but is not provided.",
                        node.sourceElement
                    )
                    validationFailed = true
                }
            }
        }

        if (validationFailed) return

        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()

        providerMap.values.distinct().forEach { node ->
            if (hasCycle(node.concreteType, visited, recursionStack)) {
                validationFailed = true
                return@forEach
            }
        }
    }

    private fun hasCycle(
        concreteType: String,
        visited: MutableSet<String>,
        recursionStack: MutableSet<String>
    ): Boolean {
        if (concreteType in recursionStack) {
            logger.error(
                "Circular dependency detected: ... -> $concreteType",
                providerMap[concreteType]?.sourceElement
            )
            return true
        }
        if (concreteType in visited) return false

        recursionStack.add(concreteType)

        val node = providerMap[concreteType] ?: return false

        node.requiredDependencies.forEach { dependencyType ->
            val dependencyNode = providerMap[dependencyType]
            if (dependencyNode != null && hasCycle(
                    dependencyNode.concreteType,
                    visited,
                    recursionStack
                )
            ) {
                logger.error("    ... which is required by '$concreteType'", node.sourceElement)
                return true
            }
        }

        visited.add(concreteType)
        recursionStack.remove(concreteType)
        return false
    }
}