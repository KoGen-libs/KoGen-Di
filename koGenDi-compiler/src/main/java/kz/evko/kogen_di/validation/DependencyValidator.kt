package kz.evko.kogen_di.validation

import com.google.devtools.ksp.processing.KSPLogger

/**
 * Compile-time check over the whole DI graph, run once with every `@KoGenComponent`/`@KoGenBean`/
 * `@KoGenViewModel` declaration in [allProviders]: every required dependency must be satisfiable
 * by exactly one provider *under the same qualifier it requests* (see
 * [validateMissingDependencies]/[validateAmbiguousDependencies]), and no two declarations may
 * register the same type under the same *explicit* qualifier (see [validateDuplicateProviders]).
 * Reports failures via [logger] (KSP compile errors) rather than throwing, so every problem in the
 * graph is reported in one pass instead of stopping at the first one found.
 */
class DependencyValidator(
    private val allProviders: List<ProviderNode>,
    private val logger: KSPLogger
) {
    /** Every provider, keyed by (satisfiable type, that provider's own qualifier) - a [RequiredDependency] is looked up by the exact same pair. */
    private val providersByKey: Map<Pair<String, String>, List<ProviderNode>>

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
        val mutableProvidersByKey = mutableMapOf<Pair<String, String>, MutableList<ProviderNode>>()

        allProviders.forEach { node ->
            node.satisfiableTypes.forEach { type ->
                mutableProvidersByKey.getOrPut(type to node.qualifier) { mutableListOf() }.add(node)
            }
        }
        providersByKey = mutableProvidersByKey
    }

    /**
     * Runs all three checks, skipping [validateAmbiguousDependencies] once [validateDuplicateProviders]
     * or [validateMissingDependencies] already failed - reporting who's ambiguous for a (type,
     * qualifier) that's either duplicated at the source or missing outright isn't useful, and for a
     * duplicated *qualified* pair it would just restate the same problem [validateDuplicateProviders]
     * already reported.
     */
    fun validate() {
        validateDuplicateProviders()
        validateMissingDependencies()
        if (validationFailed) return

        validateAmbiguousDependencies()
    }

    /**
     * Two *different* declarations registered under the exact same type and the exact same
     * *non-empty* qualifier - they'd silently collide in the generated lookup map (the later one
     * wins, the earlier is unreachable), so this is always an error, independent of whether
     * anything actually requests that (type, qualifier) pair.
     *
     * An empty qualifier is deliberately exempt: many types are legitimately satisfied by more than
     * one component/bean (e.g. several implementations of the same interface, none of them
     * requested by anything unqualified) and that's fine - see [validateAmbiguousDependencies],
     * which only flags that case when something actually requests it unqualified.
     */
    private fun validateDuplicateProviders() {
        providersByKey.forEach { (key, nodes) ->
            val (type, qualifier) = key
            if (qualifier.isEmpty()) return@forEach

            val candidates = nodes.distinct()
            if (candidates.size > 1) {
                logger.error(
                    "Duplicate provider: Type '$type' (qualifier = '$qualifier') is provided by multiple declarations: " +
                        candidates.joinToString(", ") { it.concreteType },
                    candidates.first().sourceElement
                )
                validationFailed = true
            }
        }
    }

    /** Every provider's required dependency must resolve to at least one entry in [providersByKey] under the exact (type, qualifier) it requests (or be [externalProviders]). */
    private fun validateMissingDependencies() {
        allProviders.distinct().forEach { node ->
            node.requiredDependencies.forEach { dependency ->
                val key = dependency.type to dependency.qualifier
                if (key !in providersByKey && dependency.type !in externalProviders) {
                    logger.error(
                        "Missing dependency: '${dependency.describe()}' is required by '${node.concreteType}' but is not provided.",
                        node.sourceElement
                    )
                    validationFailed = true
                }
            }
        }
    }

    /** Every requested (non-[ignoredAmbiguityTypes]/[externalProviders]) (type, qualifier) pair must resolve to exactly one provider - more than one means `inject()` would have no way to pick between them. */
    private fun validateAmbiguousDependencies() {
        val requestedDependencies = allProviders.flatMap { it.requiredDependencies }.toSet()

        requestedDependencies.forEach { dependency ->
            if (dependency.type in ignoredAmbiguityTypes || dependency.type in externalProviders) return@forEach

            val candidates = providersByKey[dependency.type to dependency.qualifier].orEmpty().distinct()
            if (candidates.size > 1) {
                logger.error(
                    "Ambiguous dependency: Type '${dependency.describe()}' is required, but provided by multiple candidates: " +
                        candidates.joinToString(", ") { it.concreteType },
                    candidates.first().sourceElement
                )
                validationFailed = true
            }
        }
    }
}

/** Human-readable form for compile errors - includes the qualifier only when one was actually requested. */
private fun RequiredDependency.describe(): String =
    if (qualifier.isEmpty()) type else "$type (qualifier = '$qualifier')"
