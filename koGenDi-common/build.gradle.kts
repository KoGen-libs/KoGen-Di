plugins {
    id("java-library")
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

sourceSets.main {
    java.srcDirs("src/main/kotlin")
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])

            groupId = properties["GROUP"].toString()
            artifactId = "android-di-common"

            pom {
                name.set("KoGen DI Common")
                description.set("Shared annotations for KoGen DI")
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
    dependsOn(tasks.test)
}
tasks.withType<PublishToMavenLocal>().configureEach {
    dependsOn(tasks.test)
}
