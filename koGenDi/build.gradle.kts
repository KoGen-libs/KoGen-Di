import org.jreleaser.model.Active

plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kspAndroid)
    alias(libs.plugins.jreleaser)
    id("maven-publish")
}

group = project.properties["GROUP"].toString()
version = project.properties["VERSION_NAME"].toString()

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

jreleaser {
    gitRootSearch = true
    environment {
        setVariables("${rootDir.path}/config.toml")
    }
    project {
        inceptionYear = "2025"
        author("@KoGen")
    }
    release {
        github {
            skipRelease = true
            skipTag = true
            sign = true
            branch = "master"
            branchPush = "master"
            overwrite = true
        }
    }
    signing {
        active = Active.ALWAYS
        armored = true
        verify = true
    }
    deploy {
        maven {
            mavenCentral.create("sonatype") {
                active = Active.ALWAYS
                url = "https://central.sonatype.com/api/v1/publisher"
                stagingRepository(layout.buildDirectory.dir("staging-deploy").get().toString())
                setAuthorization("Basic")
                retryDelay = 60
            }
        }
    }
}