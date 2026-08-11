package kz.evko.kogen_di.gradle

import org.gradle.api.provider.Property

/**
 * `koGenDi { }` - typed configuration for the KoGen DI KSP compiler, registered by
 * [KoGenDiPlugin]. Every property is optional; leaving one unset keeps the compiler's own
 * default for it. Forwarded 1:1 into the equivalent `ksp { arg(...) }` string options - this
 * extension only exists to make that configuration typed and autocompletable, not to change what
 * the compiler itself does.
 */
abstract class KoGenDiExtension {
    /**
     * Package the generated bean/component/view-model lists and inject factory are written
     * under. Defaults to inferring one from the first `@KoGenComponent`/`@KoGenBean` declaration's
     * own package if left unset.
     */
    abstract val packageName: Property<String>

    /**
     * Whether the generated inject factory includes a ViewModel-injection helper. Defaults to
     * `false` if left unset.
     */
    abstract val includeViewModelInjector: Property<Boolean>

    /**
     * Whether the generated inject factory includes a Fragment-injection helper. Defaults to
     * `false` if left unset.
     */
    abstract val includeFragmentInjector: Property<Boolean>
}
