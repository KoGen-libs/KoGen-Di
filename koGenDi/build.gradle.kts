plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("maven-publish")
    id("signing")
}

group = project.properties["GROUP"].toString()

android {
    // AGP resource/BuildConfig namespace only - unrelated to the actual Kotlin package
    // (kz.evko.kogen_di.injector/viewModel/exceptions). Kept distinct from the demo app's
    // own namespace (also kz.evko.kogen_di) to avoid an AGP namespace-collision warning.
    namespace = "kz.evko.kogen_di.runtime"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        // Deliberately lower than the other modules' JVM 17 (see koGenDi-common/koGenDi-compiler):
        // this module now ships real `inline fun`s (inject()/koGenViewModel()/...) whose bytecode
        // gets inlined directly into every consuming app's own compilation, and Kotlin refuses to
        // inline bytecode built for a higher JVM target than the consumer's own - so this must stay
        // at or below the lowest JVM target any consumer (e.g. the demo `app`, JVM 1.8) might use.
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    api(project(":koGenDi-common"))

    // compileOnly - the ViewModel/Compose/Fragment entry points are opt-in (the consuming
    // module's own KSP flags decide whether they're even generated), so a plain-DI-only consumer
    // must not be forced to pull in Compose/Fragment/lifecycle-viewmodel transitively.
    compileOnly(libs.androidx.viewmodel.android)
    compileOnly(platform(libs.androidx.compose.bom))
    compileOnly(libs.androidx.compose.runtime)
    compileOnly(libs.androidx.fragment.ktx)
    compileOnly(libs.androidx.activity)

    // compileOnly doesn't extend to the test source set on its own, and the Compose compiler
    // plugin (buildFeatures.compose = true above) requires the runtime on the classpath for any
    // Kotlin compile task in this module, tests included, even though no test here is itself
    // @Composable.
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.runtime)
    testImplementation(libs.junit)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = properties["GROUP"].toString()
                artifactId = "android-di"

                pom {
                    name.set("KoGen DI")
                    description.set("The best DI for Android)")
                    url.set("https://github.com/EugenProg/KoGen-Di")

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
        }
        repositories {
            maven {
                setUrl(layout.buildDirectory.dir("staging-deploy"))
            }
        }
    }

    val signingKey: String? = System.getenv("JRELEASER_GPG_SECRET_KEY")
    val signingPassword: String? = System.getenv("JRELEASER_GPG_PASSPHRASE")
    if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
        signing {
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(publishing.publications["release"])
        }
    }

    tasks.withType<PublishToMavenRepository>().configureEach {
        dependsOn(tasks.named("test"))
    }
    tasks.withType<PublishToMavenLocal>().configureEach {
        dependsOn(tasks.named("test"))
    }
}
