plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kspAndroid)
    id("maven-publish")
}

group = properties["GROUP"].toString()
version = properties["VERSION_NAME"].toString()

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

sourceSets.main {
    java.srcDirs("src/main/kotlin")
}

dependencies {
    implementation(libs.symbol.processing)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])

            groupId = properties["GROUP"].toString()
            artifactId = "android-di"

            pom {
                name.set(project.properties["POM_NAME"].toString())
                description.set(project.description)

                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("EugenProg")
                        name.set("Eugen Kopp")
                        email.set("Eugen.kopp.kz@gmail.com")
                    }
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