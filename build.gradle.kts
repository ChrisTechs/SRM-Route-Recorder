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

        val javaHome = System.getProperty("java.home")
        val modules = listOf(
            "java.base", "java.logging", "java.desktop", "java.management",
            "java.naming", "java.rmi", "java.scripting", "java.sql", "java.xml",
            "jdk.jfr", "jdk.unsupported", "jdk.management", "jdk.net"
        )

        modules.forEach { mod ->
            val filter = if (mod == "java.base") mapOf("jarfilter" to "!**.jar", "filter" to "!module-info.class") else emptyMap()
            val jmodFile = File("$javaHome/jmods/$mod.jmod")

            if (jmodFile.exists()) {
                libraryjars(filter, jmodFile.absolutePath)
            }
        }

        configuration("proguard-rules.pro")
        verbose()
    }

    build {
        dependsOn("minifyJar")
    }
}