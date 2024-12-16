plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kspAndroid)
    id("maven-publish")
}

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
        register<MavenPublication>("release") {
            groupId = "kz.evko.koGen"
            artifactId = "di"
            version = System.getenv("VERSION") ?: "1.0.0"

            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "di_repo"
            url = uri("${projectDir.path}/repo")
        }
    }
}