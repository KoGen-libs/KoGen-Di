package kz.evko.kogen_di.validation

import com.google.devtools.ksp.processing.KSPLogger

/**
 * Compile-time check over the whole DI graph, run once with every `@KoGenComponent`/`@KoGenBean`/
 * `@KoGenViewModel` declaration in [allProviders]: every required dependency must be satisfiable
 * by exactly one provider (see [validateMissingDependencies]/[validateAmbiguousDependencies]).
 * Reports failures via [logger] (KSP compile errors) rather than throwing, so every problem in the
 * graph is reported in one pass instead of stopping at the first one found.
 */
class DependencyValidator(
    private val allProviders: List<ProviderNode>,
    private val logger: KSPLogger
) {
    private val providersByType: Map<String, List<ProviderNode>>

    // Provided by inject<Context>() at runtime (via setApplicationContext), not by any
    // @KoGenComponent/@KoGenBean - so it must be exempted from both checks below or every
    // Context-taking constructor would be flagged as missing a provider.
    private val externalProviders = setOf("android.content.Context")

    // Broad supertypes many unrelated components/ViewModels satisfy at once (every ViewModel
    // extends ViewModel, many DTOs implement Serializable, everything is Any) - a real ambiguity
    // check on these would fire constantly on totally unrelated types and be useless noise.
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

    /** Runs both checks, skipping [validateAmbiguousDependencies] if a dependency is missing outright - reporting who's ambiguous for a type nothing can even satisfy isn't useful. */
    fun validate() {
        validateMissingDependencies()
        if (validationFailed) return

        validateAmbiguousDependencies()
    }

    /** Every provider's required dependency must resolve to at least one entry in [providersByType] (or be [externalProviders]). */
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

    /** Every requested (non-[ignoredAmbiguityTypes]/[externalProviders]) type must resolve to exactly one provider - more than one means `inject()` would have no way to pick between them. */
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
