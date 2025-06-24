plugins {
    id("dev.kikugie.stonecutter")
    id("fabric-loom") version "1.10-SNAPSHOT" apply false
    //id("dev.kikugie.j52j") version "1.0.2" apply false // Enables asset processing by writing json5 files
    id("me.modmuss50.mod-publish-plugin") version "0.7.+" apply false // Publishes builds to hosting websites
}
stonecutter active "dev" /* [SC] DO NOT EDIT */

stonecutter.tasks {
    order("publishModrinth")
    //order("publishCurseforge")
}

//region Run Active Configurations
tasks.register("runActiveClient") {
    group = "stonecutter-impl"
    description = "Runs the active project client"
    dependsOn(":${stonecutter.current!!.project}:runClient")
}

tasks.register("runActiveServer") {
    group = "stonecutter-impl"
    description = "Runs the active project server"
    dependsOn(":${stonecutter.current!!.project}:runServer")
}

tasks.register("generateIdeaRunConfigs") {
    group = "stonecutter-impl"
    description = "Generates IntelliJ run configurations for Run Active Client/Server"
    doLast {
        val ideaDir = file("${rootProject.projectDir}/.idea/runConfigurations")
        ideaDir.mkdirs()
        file("${ideaDir}/Stonecutter_Run_Active_Client.xml").writeText(
            "<configuration name=\"Run Active Client\" type=\"GradleRunConfiguration\" factoryName=\"Gradle\" folderName=\"Stonecutter\">\n" +
            "    <ExternalSystemSettings>\n" +
            "        <option name=\"executionName\"/>\n" +
            "        <option name=\"externalProjectPath\" value=\"\$PROJECT_DIR\$\"/>\n" +
            "        <option name=\"externalSystemIdString\" value=\"GRADLE\"/>\n" +
            "        <option name=\"scriptParameters\"/>\n" +
            "        <option name=\"taskDescriptions\">\n" +
            "            <list/>\n" +
            "        </option>\n" +
            "        <option name=\"taskNames\">\n" +
            "            <list>\n" +
            "                <option value=\":runActiveClient\"/>\n" +
            "            </list>\n" +
            "        </option>\n" +
            "        <option name=\"vmOptions\"/>\n" +
            "    </ExternalSystemSettings>\n" +
            "    <ExternalSystemDebugServerProcess>true</ExternalSystemDebugServerProcess>\n" +
            "    <ExternalSystemReattachDebugProcess>true</ExternalSystemReattachDebugProcess>\n" +
            "    <DebugAllEnabled>false</DebugAllEnabled>\n" +
            "    <RunAsTest>false</RunAsTest>\n" +
            "    <method v=\"2\"/>\n" +
            "</configuration>\n"
        )
        file("${ideaDir}/Stonecutter_Run_Active_Server.xml").writeText(
            "<configuration name=\"Run Active Server\" type=\"GradleRunConfiguration\" factoryName=\"Gradle\" folderName=\"Stonecutter\">\n" +
            "    <ExternalSystemSettings>\n" +
            "        <option name=\"executionName\"/>\n" +
            "        <option name=\"externalProjectPath\" value=\"\$PROJECT_DIR\$\"/>\n" +
            "        <option name=\"externalSystemIdString\" value=\"GRADLE\"/>\n" +
            "        <option name=\"scriptParameters\"/>\n" +
            "        <option name=\"taskDescriptions\">\n" +
            "            <list/>\n" +
            "        </option>\n" +
            "        <option name=\"taskNames\">\n" +
            "            <list>\n" +
            "                <option value=\":runActiveServer\"/>\n" +
            "            </list>\n" +
            "        </option>\n" +
            "        <option name=\"vmOptions\"/>\n" +
            "    </ExternalSystemSettings>\n" +
            "    <ExternalSystemDebugServerProcess>true</ExternalSystemDebugServerProcess>\n" +
            "    <ExternalSystemReattachDebugProcess>true</ExternalSystemReattachDebugProcess>\n" +
            "    <DebugAllEnabled>false</DebugAllEnabled>\n" +
            "    <RunAsTest>false</RunAsTest>\n" +
            "    <method v=\"2\"/>\n" +
            "</configuration>\n"
        )
    }
}

// Automatically generate run configs after project evaluation (after sync)
gradle.projectsEvaluated {
    tasks["generateIdeaRunConfigs"].actions.forEach { action ->
        action.execute(tasks["generateIdeaRunConfigs"])
    }
}
//endregion