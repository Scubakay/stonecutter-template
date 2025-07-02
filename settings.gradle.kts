pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/snapshots")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.7-beta.3"
}

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    shared {
        vers("dev", "1.21.7")
        versions("1.21.6")
        vcsVersion = "dev"
    }
    create(rootProject)
}

//region Git precommit hook
gradle.beforeProject {
    val gitDir = rootDir.resolve(".git")
    if (gitDir.exists() && gitDir.isDirectory) {
        val hooksDir = gitDir.resolve("hooks")
        val preCommitHook = hooksDir.resolve("pre-commit")

        if (!preCommitHook.exists()) {
            hooksDir.mkdirs()
            preCommitHook.writeText(
                """
                #!/bin/bash
                
                vcs_version=$(ggrep -oP 'vcsVersion\s*=\s*"\K[^"]+' settings.gradle.kts)
                active_version=$(ggrep -oP 'stonecutter\s+active\s+"\K[^"]+' stonecutter.gradle.kts)
                
                echo "Detected vcsVersion: ${'$'}vcs_version"
                echo "Detected active version: ${'$'}active_version"
                
                if [ "${'$'}vcs_version" != "${'$'}active_version" ]; then
                  echo "Please run './gradlew \"Reset active project\"' to set the stonecutter branch to the version control version."
                  exit 1
                else
                  echo "Versions match. No action needed."
                fi
                """.trimIndent()
            )
            preCommitHook.setExecutable(true)
            println("Git pre-commit hook installed.")
        }
    } else {
        println("Not a Git repository. Skipping hook installation.")
    }
}
//endregion

rootProject.name = "Stonecutter Template"