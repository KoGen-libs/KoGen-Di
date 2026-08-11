plugins {
    `java-gradle-plugin`
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.jreleaser)
    id("maven-publish")
    id("signing")
}

group = project.properties["GROUP"].toString()

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    plugins {
        create("koGenDi") {
            id = "io.github.eugenprog.kogen-di"
            displayName = "KoGen DI"
            description = "Typed Gradle DSL for the KoGen DI KSP compiler - " +
                "replaces string-based ksp { arg(...) } options with a real koGenDi { } block."
            implementationClass = "kz.evko.kogen_di.gradle.KoGenDiPlugin"
        }
    }
}

// The plugin needs to know its own published version at runtime, to add matching
// implementation()/ksp() dependencies on the rest of the library (see KoGenDiPlugin). A generated
// resource, rather than reading the jar manifest, since Gradle doesn't stamp Implementation-Version
// into the manifest by default and this avoids relying on that.
val versionPropertiesDir = layout.buildDirectory.dir("generated/version-properties")
val writeVersionProperties = tasks.register("writeVersionProperties") {
    val outputDir = versionPropertiesDir
    val versionValue = project.version.toString()
    inputs.property("version", versionValue)
    outputs.dir(outputDir)
    doLast {
        outputDir.get().asFile.apply {
            mkdirs()
            resolve("kogen-di-plugin-version.properties").writeText("version=$versionValue")
        }
    }
}

sourceSets.main {
    resources.srcDir(versionPropertiesDir)
}

tasks.named("processResources") {
    dependsOn(writeVersionProperties)
}
// withSourcesJar()'s task also picks up the generated resources dir (it's on the main source
// set), so it needs the same explicit dependency to avoid Gradle's implicit-dependency warning.
tasks.named("sourcesJar") {
    dependsOn(writeVersionProperties)
}

dependencies {
    // compileOnly, deliberately: at runtime this must be whatever KSP Gradle plugin version the
    // *consumer* applied themselves (matching their own Kotlin version) - bundling one here would
    // risk silently conflicting with it. See KoGenDiPlugin's doc comment.
    compileOnly(libs.symbol.processing.gradle.plugin)

    testImplementation(libs.junit)
    testImplementation(gradleTestKit())
    // Real KSP + Kotlin Gradle plugins on the *test* classpath only, so tests can apply them for
    // real via ProjectBuilder instead of faking the integration - kept off the plugin's own
    // compileOnly/runtime classpath deliberately (see KoGenDiPlugin's doc comment).
    testImplementation(libs.symbol.processing.gradle.plugin)
    testImplementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")

    constraints {
        implementation("org.apache.commons:commons-compress:1.26.2") {
            because("JReleaser requires this version to avoid a conflict")
        }
    }
}

// java-gradle-plugin only wires up its "pluginMaven"/marker publications once the project is
// fully evaluated (it reacts to maven-publish being applied internally), so configuring them
// eagerly at script-evaluation time fails with "Publication ... not found" - hence afterEvaluate.
afterEvaluate {
    publishing.publications.named<MavenPublication>("pluginMaven") {
        artifactId = "android-di-gradle-plugin"
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("KoGen DI Gradle Plugin")
            description.set("Typed Gradle DSL for the KoGen DI KSP compiler")
            url.set("https://github.com/EugenProg/KoGen-DI_demo")

            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }

            developers {
                developer {
                    id.set("EugenProg")
                    name.set("Eugen Kopp")
                    email.set("Eugen.kopp.kz@gmail.com")
                }
            }

            scm {
                connection.set("scm:git:git://github.com/EugenProg/KoGen-Di.git")
                developerConnection.set("scm:git:ssh://github.com:EugenProg/KoGen-Di.git")
                url.set("https://github.com/EugenProg/KoGen-Di/tree/master")
            }
        }
    }
    repositories {
        maven {
            setUrl(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

val signingKey = System.getenv("JRELEASER_GPG_SECRET_KEY")
val signingPassword = System.getenv("JRELEASER_GPG_PASSPHRASE")
if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
    signing {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}
