package kz.evko.kogen_di.gradle

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.Properties

private const val KSP_PLUGIN_ID = "com.google.devtools.ksp"

/**
 * Registers the `koGenDi { }` DSL (see [KoGenDiExtension]), forwards its typed properties into
 * KSP's own `ksp { arg(...) }` options, and adds this library's own runtime/compiler dependencies
 * at the matching version.
 *
 * Deliberately does *not* apply [KSP_PLUGIN_ID] itself: KSP's version is tied tightly to the
 * consuming project's own Kotlin version; bundling one here would risk a silent version mismatch
 * surfacing as a confusing crash inside KSP's own code, rather than a clear error from us. The
 * consumer applies KSP themselves - as most Android/Kotlin projects already do for other reasons
 * - and this plugin just requires it to already be present, erroring plainly if it isn't.
 */
class KoGenDiPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("koGenDi", KoGenDiExtension::class.java)
        extension.includeViewModelInjector.convention(false)
        extension.includeFragmentInjector.convention(false)

        // Dependencies are added as soon as KSP is applied (via withPlugin), not deferred to
        // afterEvaluate - deferring it that late is a real bug found while building the sibling
        // koGenNavigation plugin: on Android, KSP decides per-variant (kspDebug/kspRelease/...)
        // whether there's anything to process quite early, and a "ksp" dependency added only in
        // afterEvaluate arrived too late for it to notice - the flat "ksp" configuration ended up
        // with our compiler dependency, but "kspDebug" stayed empty and no kspDebugKotlin task
        // was ever created at all.
        project.pluginManager.withPlugin(KSP_PLUGIN_ID) {
            val version = pluginVersion()
            project.dependencies.add("implementation", "io.github.eugenprog:android-di:$version")
            project.dependencies.add("ksp", "io.github.eugenprog:android-di-compiler:$version")
        }

        // The rest - ksp.arg(...) forwarding - isn't timing-sensitive the same way (it only
        // populates a lazily-read option map), so it's safe (and necessary) to do from
        // afterEvaluate, once the user's own koGenDi { } block has definitely already run.
        project.afterEvaluate {
            if (!project.plugins.hasPlugin(KSP_PLUGIN_ID)) {
                throw GradleException(
                    "koGenDi requires the KSP plugin. Apply '$KSP_PLUGIN_ID' " +
                        "(a version matching your project's Kotlin version) before applying koGenDi.",
                )
            }

            val ksp = project.extensions.getByType(KspExtension::class.java)
            // packageName is only forwarded when actually present - the compiler's own
            // "infer from the first annotated declaration" fallback only kicks in when the option
            // is absent entirely; forwarding an empty string would silently break that.
            if (extension.packageName.isPresent) ksp.arg("packageName", extension.packageName.get())
            ksp.arg("includeViewModelInjector", extension.includeViewModelInjector.get().toString())
            ksp.arg("includeFragmentInjector", extension.includeFragmentInjector.get().toString())
        }
    }

    /**
     * This plugin's own published version, read from a resource generated at build time (see
     * `writeVersionProperties` in this module's build.gradle.kts) - used to pull in the matching
     * runtime/compiler versions, since all three are always released together under the same
     * version number.
     */
    private fun pluginVersion(): String {
        val resource = javaClass.classLoader
            .getResourceAsStream("kogen-di-plugin-version.properties")
            ?: throw GradleException(
                "koGenDi: couldn't find its own version-properties resource - this indicates a " +
                    "broken build of the plugin itself, not a problem with your project.",
            )
        return resource.use { stream ->
            Properties().apply { load(stream) }.getProperty("version")
                ?: throw GradleException("koGenDi: version-properties resource has no 'version' entry.")
        }
    }
}
