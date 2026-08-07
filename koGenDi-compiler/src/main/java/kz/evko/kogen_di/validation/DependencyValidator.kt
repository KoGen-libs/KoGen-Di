package kz.evko.kogen_di.validation

import com.google.devtools.ksp.processing.KSPLogger

class DependencyValidator(
    private val allProviders: List<ProviderNode>,
    private val logger: KSPLogger
) {
    private val providersByType: Map<String, List<ProviderNode>>
    private val externalProviders = setOf("android.content.Context")

    private val ignoredAmbiguityTypes = setOf(
        "androidx.lifecycle.ViewModel",
        "java.io.Serializable",
        "kotlin.Any",
    )
    private var validationFailed = false

    init {
        val mutableProvidersByType = mutableMapOf<String, MutableList<ProviderNode>>()

        allProviders.forEach { node ->
            node.satisfiableTypes.forEach { type ->
                mutableProvidersByType.getOrPut(type) { mutableListOf() }.add(node)
            }
        }
        providersByType = mutableProvidersByType
    }

    fun validate() {
        validateMissingDependencies()
        if (validationFailed) return

        validateAmbiguousDependencies()
    }

    private fun validateMissingDependencies() {
        allProviders.distinct().forEach { node ->
            node.requiredDependencies.forEach { dependency ->
                if (dependency !in providersByType && dependency !in externalProviders) {
                    logger.error(
                        "Missing dependency: '$dependency' is required by '${node.concreteType}' but is not provided.",
                        node.sourceElement
                    )
                    validationFailed = true
                }
            }
        }
    }

    private fun validateAmbiguousDependencies() {
        val requestedTypes = allProviders.flatMap { it.requiredDependencies }.toSet()

        requestedTypes.forEach { type ->
            if (type in ignoredAmbiguityTypes || type in externalProviders) return@forEach

            val candidates = providersByType[type].orEmpty().distinct()
            if (candidates.size > 1) {
                logger.error(
                    "Ambiguous dependency: Type '$type' is required, but provided by multiple candidates: " +
                        candidates.joinToString(", ") { it.concreteType },
                    candidates.first().sourceElement
                )
                validationFailed = true
            }
        }
    }
}
