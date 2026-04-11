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
    implementation("it.unimi.dsi:fastutil:8.5.12")
}

tasks {
    shadowJar {
        manifest {
            attributes("Main-Class" to "io.github.christechs.routerec.RouteRecorderServer")
        }
    }

    build {
        dependsOn(shadowJar)
    }
}