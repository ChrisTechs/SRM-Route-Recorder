import proguard.gradle.ProGuardTask

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.9.1")
    }
}

plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.0"
}

group = "io.github.christechs.routerec"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.minestom:minestom:2026.03.25-1.21.11")
    implementation("dev.hollowcube:polar:1.15.0")
    implementation("it.unimi.dsi:fastutil:8.5.18")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks {
    shadowJar {
        archiveClassifier.set("all")
        entryCompression = ZipEntryCompression.DEFLATED

        manifest {
            attributes("Main-Class" to "io.github.christechs.routerec.RouteRecorderServer")
        }
    }

    register<ProGuardTask>("minifyJar") {
        dependsOn(shadowJar)

        injars(shadowJar.get().archiveFile)
        outjars(layout.buildDirectory.file("libs/${project.name}-${project.version}-minified.jar"))

        val service = project.extensions.getByType<JavaToolchainService>()
        val javaExtension = project.extensions.getByType<JavaPluginExtension>()

        val toolchainPath = service.launcherFor(javaExtension.toolchain)
            .get().metadata.installationPath.asFile

        val jmodsDir = File(toolchainPath, "jmods")

        if (jmodsDir.exists()) {
            jmodsDir.listFiles { f: File -> f.name.endsWith(".jmod") }?.forEach { file ->
                val filter = if (file.name == "java.base.jmod") {
                    mapOf("jarfilter" to "!**.jar", "filter" to "!module-info.class")
                } else emptyMap<String, String>()

                libraryjars(filter, file.absolutePath)
            }
        }

        configuration("proguard-rules.pro")
        verbose()
    }

    build {
        dependsOn("minifyJar")
    }
}