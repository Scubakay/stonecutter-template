pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://repo.spongepowered.org/maven")
        maven("https://maven.kikugie.dev/snapshots")
        maven("https://maven.kikugie.dev/releases")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.7-beta.3"
}

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    shared {
        // Helper function to declare vers for a Minecraft version and multiple loaders
        fun mc(version: String, vararg loaders: String) {
            for (loader in loaders) {
                vers("$version-$loader", version)
            }
        }
        // Replace individual vers calls with mc
        vers("dev", "1.21.6")
        mc("1.21.6", "fabric", "neoforge")
        vcsVersion = "dev"
    }
    create(rootProject)
}

rootProject.name = "Template"