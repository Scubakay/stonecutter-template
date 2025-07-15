plugins {
    id("dev.kikugie.stonecutter")
    id("fabric-loom") version "1.10-SNAPSHOT" apply false
    //id("dev.kikugie.j52j") version "1.0.2" apply false // Enables asset processing by writing json5 files
    id("me.modmuss50.mod-publish-plugin") version "0.8.4" apply false // Publishes builds to hosting websites
}
stonecutter active "1.21.6" /* [SC] DO NOT EDIT */

stonecutter.tasks {
    order("publishModrinth")
    //order("publishCurseforge")
}

gradle.projectsEvaluated {
    tasks["generateIdeaRunConfigs"].actions.forEach { action ->
        action.execute(tasks["generateIdeaRunConfigs"])
    }
    tasks["installPreCommitHook"].actions.forEach { action ->
        action.execute(tasks["installPreCommitHook"])
    }
}

//region Run Active Configurations
/**
 * Run configurations for active client/server
 */
tasks.register("stonecutterRunActiveClient") {
    group = "stonecutter"
    description = "Runs the active project client"
    dependsOn(":${stonecutter.current!!.project}:runClient")
}

tasks.register("stonecutterRunActiveServer") {
    group = "stonecutter"
    description = "Runs the active project server"
    dependsOn(":${stonecutter.current!!.project}:runServer")
}

tasks.register("generateIdeaRunConfigs") {
    group = "stonecutter-impl"
    description = "Generates IntelliJ run configurations for Run Active Client/Server"
    doLast {
        val ideaDir = file("${rootProject.projectDir}/.idea/runConfigurations")
        ideaDir.mkdirs()

        val activeProject = stonecutter.current!!.project
        val projectDisplay = rootProject.name.replace(' ', '_')
        val clientModuleName = "$projectDisplay.$activeProject.client"
        val serverModuleName = "$projectDisplay.$activeProject.main"
        val runDir = "\$PROJECT_DIR\$/versions\\$activeProject/../../run/"
        val loomCache = "versions\\$activeProject\\.gradle\\loom-cache\\launch.cfg"
        val projectPath = rootProject.projectDir.absolutePath.replace("\\", "/")

        val template = file("${rootProject.projectDir}/idea_config.xml").readText()

        // Client config
        val clientConfig = template
            .replace("%ENTRY_NAME%", "Run Active Client")
            .replace("%MAIN_CLASS%", "net.fabricmc.devlaunchinjector.Main")
            .replace("%MODULE_NAME%", clientModuleName)
            .replace("%PROGRAM_PARAMETERS%", "")
            .replace("%VM_PARAMETERS%", "-Dfabric.dli.config=$projectPath/$loomCache -Dfabric.dli.env=client -Dmixin.debug.export=true -Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient")
            .replace("%WORKING_DIRECTORY%", runDir)
        file("${ideaDir}/Stonecutter_runActiveClient.xml").writeText(clientConfig)

        // Server config
        val serverConfig = template
            .replace("%ENTRY_NAME%", "Run Active Server")
            .replace("%MAIN_CLASS%", "net.fabricmc.devlaunchinjector.Main")
            .replace("%MODULE_NAME%", serverModuleName)
            .replace("%PROGRAM_PARAMETERS%", "")
            .replace("%VM_PARAMETERS%", "-Dfabric.dli.config=$projectPath/$loomCache -Dfabric.dli.env=server -Dmixin.debug.export=true -Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotServer")
            .replace("%WORKING_DIRECTORY%", runDir)
        file("${ideaDir}/Stonecutter_runActiveServer.xml").writeText(serverConfig)
    }
}
//endregion

//region Git precommit hook
/**
 * Register a git precommit hook that checks if the active version is set to the vcs version
 */
tasks.register("installPreCommitHook") {
    group = "git"
    description = "Installs a git pre-commit hook to check active version against vcs version"
    doLast {
        val gitDir = rootDir.resolve(".git")
        if (gitDir.exists() && gitDir.isDirectory) {
            val hooksDir = gitDir.resolve("hooks")
            val preCommitHook = hooksDir.resolve("pre-commit")

            if (!preCommitHook.exists()) {
                hooksDir.mkdirs()
                preCommitHook.writeText(
                    """
                    #!/bin/bash

                    vcs_version=$(ggrep -oP '^settings\.vcsVersion\s*=\s*\K[^ \r\n]+' gradle.properties)
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
}
//endregion