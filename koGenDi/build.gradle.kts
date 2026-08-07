plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.jreleaser)
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
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
