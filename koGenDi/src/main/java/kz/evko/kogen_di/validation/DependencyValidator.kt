package kz.evko.kogen_di.validation

import com.google.devtools.ksp.processing.KSPLogger

class DependencyValidator(
    allProviders: List<ProviderNode>,
    private val logger: KSPLogger
) {
    private val providerMap: Map<String, ProviderNode>
    private val externalProviders = setOf("android.content.Context")
    private var validationFailed = false

    init {
        val mutableProviderMap = mutableMapOf<String, ProviderNode>()

        allProviders.forEach { node ->
            node.satisfiableTypes.forEach { type ->
                    mutableProviderMap[type] = node
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
    }
}