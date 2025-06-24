import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `maven-publish`
    id("fabric-loom")
    //id("dev.kikugie.j52j")
    id("me.modmuss50.mod-publish-plugin")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

class ModData {
    val id = property("mod.id").toString()
    val name = property("mod.name").toString()
    val title = property("mod.mc_title").toString()
    val version = property("mod.version").toString()
    val group = property("mod.group").toString()
    val publish = property("mod.publish").toString().toBoolean() && id != "template"
    val mcDep = property("mod.mc_dep").toString()
}

class ModDependencies(private val prefix: String) {
    operator fun get(name: String) = property("$prefix.$name").toString()
    fun checkSpecified(depName: String): Boolean {
        val property = findProperty("$prefix.$depName")
        return property != null && property != "[VERSIONED]"
    }
}

fun modules(): Array<String> {
    return (findProperty("deps.fabric_modules") as? String)
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.toTypedArray()
        ?: emptyArray()
}

val mod = ModData()
val deps = ModDependencies("deps")
val dev = ModDependencies("dev")
val mcVersion = stonecutter.current.version

version = "${mod.version}+${mod.title}"
group = mod.group
base { archivesName.set(mod.id) }

loom {
    splitEnvironmentSourceSets()

    mods {
        create("template") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }
}

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings("net.fabricmc:yarn:$mcVersion+build.${deps["yarn_build"]}:v2")
    modImplementation("net.fabricmc:fabric-loader:${deps["fabric_loader"]}")

    // Include Fabric api or individually specified modules
    if(deps.checkSpecified("fabric_api")) {
        if (deps.checkSpecified("fabric_modules"))
            modules().forEach {modImplementation(fabricApi.module(it, deps["fabric_api"]))}
        else modImplementation("net.fabricmc.fabric-api:fabric-api:${deps["fabric_api"]}")
    }

    if (dev.checkSpecified("modmenu"))
        modLocalRuntime("maven.modrinth:modmenu:${dev["modmenu"]}-fabric")
}

//region Shadow libraries

/**
 * Example:
 * shadowLibrary("de.maxhenkel.configbuilder:configbuilder:${deps["henkel_config"]}")
 */
val shadowLibrary: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shadowLibrary)
    archiveClassifier = "dev-shadow"
    // Relocate stuff here:
    // relocate("de.maxhenkel.admiral", "com.scubakay.autorelog.admiral")
}

tasks {
    remapJar {
        inputFile = shadowJar.get().archiveFile
    }
}

//endregion

loom {
    decompilers {
        get("vineflower").apply { // Adds names to lambdas - useful for mixins
            options.put("mark-corresponding-synthetics", "1")
        }
    }

    runConfigs.all {
        ideConfigGenerated(true)
        vmArgs("-Dmixin.debug.export=true")
        runDir = "../../run"
    }
}

java {
    withSourcesJar()
    val java = if (stonecutter.eval(mcVersion, ">=1.20.6")) JavaVersion.VERSION_21 else JavaVersion.VERSION_17
    targetCompatibility = java
    sourceCompatibility = java
}

tasks.processResources {
    inputs.property("id", mod.id)
    inputs.property("name", mod.name)
    inputs.property("version", mod.version)
    inputs.property("mcdep", mod.mcDep)

    val map = mapOf(
        "id" to mod.id,
        "name" to mod.name,
        "version" to mod.version,
        "mcdep" to mod.mcDep
    )

    filesMatching("fabric.mod.json") { expand(map) }
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    from(tasks.remapJar.get().archiveFile)
    into(rootProject.layout.buildDirectory.file("libs/${mod.version}"))
    dependsOn("build")
}

publishMods {
    file = tasks.remapJar.get().archiveFile
    additionalFiles.from(tasks.remapSourcesJar.get().archiveFile)
    displayName = "${mod.name} ${mod.version} for ${mod.title}"
    version = mod.version
    changelog = rootProject.file("CHANGELOG.md").readText()
    type = STABLE
    modLoaders.add("fabric")

    dryRun = !mod.publish
            || providers.environmentVariable("MODRINTH_TOKEN").getOrNull() == null
            || providers.environmentVariable("CURSEFORGE_TOKEN").getOrNull() == null

    modrinth {
        projectId = property("publish.modrinth").toString()
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.add(mcVersion)
        requires {
            slug = "fabric-api"
        }
    }

//    Uncomment publishing order in stonecutter.gradle.kts too if you want to publish to Curseforge
//    curseforge {
//        projectId = property("publish.curseforge").toString()
//        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
//        minecraftVersions.add(mcVersion)
//        requires {
//            slug = "fabric-api"
//        }
//    }
}

publishing {
    repositories {
        maven("...") {
            name = "..."
            credentials(PasswordCredentials::class.java)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "${property("mod.group")}.${mod.id}"
            artifactId = mod.version
            version = mcVersion

            from(components["java"])
        }
    }
}