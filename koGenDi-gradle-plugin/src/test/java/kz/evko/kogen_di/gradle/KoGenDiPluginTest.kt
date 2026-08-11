package kz.evko.kogen_di.gradle

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.GradleException
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class KoGenDiPluginTest {

    @Test
    fun `fails clearly when KSP isn't applied`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(KoGenDiPlugin::class.java)

        val failure = assertThrows(GradleException::class.java) {
            (project as ProjectInternal).evaluate()
        }
        // ProjectInternal.evaluate() wraps our GradleException in a ProjectConfigurationException
        // whose own .message is a generic "problem configuring root project" - the real message
        // (and the plugin id it should name) is on the cause chain instead.
        val causeChain = generateSequence(failure as Throwable) { it.cause }
        assertTrue(
            causeChain.any { it.message.orEmpty().contains("com.google.devtools.ksp") },
        )
    }

    @Test
    fun `forwards typed extension values into the equivalent ksp arg options`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply("com.google.devtools.ksp")
        project.pluginManager.apply(KoGenDiPlugin::class.java)

        val extension = project.extensions.getByType(KoGenDiExtension::class.java)
        extension.packageName.set("com.example.di")
        extension.includeViewModelInjector.set(true)
        extension.includeFragmentInjector.set(true)

        (project as ProjectInternal).evaluate()

        val ksp = project.extensions.getByType(KspExtension::class.java)
        assertEquals("com.example.di", ksp.arguments["packageName"])
        assertEquals("true", ksp.arguments["includeViewModelInjector"])
        assertEquals("true", ksp.arguments["includeFragmentInjector"])
    }

    @Test
    fun `leaves packageName unset when not configured, instead of forwarding an empty string`() {
        // Regression guard: packageName left unset must trigger the compiler's own
        // infer-from-declaration fallback, same as if raw ksp { arg(...) } never mentioned it at
        // all - not silently forward "" and break that fallback.
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply("com.google.devtools.ksp")
        project.pluginManager.apply(KoGenDiPlugin::class.java)

        (project as ProjectInternal).evaluate()

        val ksp = project.extensions.getByType(KspExtension::class.java)
        assertTrue(!ksp.arguments.containsKey("packageName"))
    }

    @Test
    fun `defaults includeViewModelInjector and includeFragmentInjector to false when not configured`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply("com.google.devtools.ksp")
        project.pluginManager.apply(KoGenDiPlugin::class.java)

        (project as ProjectInternal).evaluate()

        val ksp = project.extensions.getByType(KspExtension::class.java)
        assertEquals("false", ksp.arguments["includeViewModelInjector"])
        assertEquals("false", ksp.arguments["includeFragmentInjector"])
    }

    @Test
    fun `adds matching implementation and ksp dependencies on its own runtime and compiler`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply("com.google.devtools.ksp")
        project.pluginManager.apply(KoGenDiPlugin::class.java)

        (project as ProjectInternal).evaluate()

        val implementationDeps = project.configurations.getByName("implementation").dependencies
        val kspDeps = project.configurations.getByName("ksp").dependencies
        assertTrue(implementationDeps.any { it.group == "io.github.eugenprog" && it.name == "android-di" })
        assertTrue(kspDeps.any { it.group == "io.github.eugenprog" && it.name == "android-di-compiler" })
    }
}
