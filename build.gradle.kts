import me.modmuss50.mpp.ReleaseType
//import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `maven-publish`
    id("fabric-loom")
    //id("dev.kikugie.j52j")
    kotlin("jvm") version "2.1.21"
    id("com.google.devtools.ksp") version "2.1.21-2.0.2"
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.9"
    id("me.modmuss50.mod-publish-plugin")
    //id("com.github.johnrengelman.shadow") version "8.1.1"
}

//region Shadow libraries
/**
 * Everything needed to shadow a library, example:
 * shadowAndRelocate("de.maxhenkel.configbuilder:configbuilder:2.0.2", "de.maxhenkel.configbuilder", "com.scubakay.autorelog.configbuilder")
 * When using: uncomment the import and plugins.id for com.github.johnrengelman.shadow
 */
//val shadowLibrary = configurations.create("shadowLibrary") {
//    isCanBeResolved = true
//    isCanBeConsumed = false
//}
//
//val shadowRelocations = mutableListOf<Pair<String, String>>()
//
//fun shadowAndRelocate(dependencyNotation: String, fromPackage: String, toPackage: String) {
//    dependencies.add("shadowLibrary", dependencyNotation)
//    shadowRelocations.add(fromPackage to toPackage)
//}
//
//tasks.named<ShadowJar>("shadowJar") {
//    configurations = listOf(shadowLibrary)
//    archiveClassifier = "dev-shadow"
//    shadowRelocations.forEach { (from, to) ->
//        relocate(from, to)
//    }
//}
//
//tasks {
//    remapJar {
//        inputFile = shadowJar.get().archiveFile
//    }
//}
//endregion

//region Mod information
class ModData {
    val version = property("mod.version").toString()
    val group = property("mod.group").toString()
    val id = property("mod.id").toString()
    val name = property("mod.name").toString()
    val description = property("mod.description").toString()
    val homepage = property("mod.homepage").toString()
    val repository = property("mod.repository").toString()
    val discord = property("mod.discord").toString()
}

class Environment {
    val range = property("mc.range").toString()
    val title = property("mc.title").toString()
    val targets = property("mc.targets").toString().split("\\s+".toRegex()).map { it.trim() }

    val channel = ReleaseType.of(property("publish.channel").toString())
    val dry_run = property("publish.dry_run").toString().toBoolean() || property("mod.id").toString() == "template"
    val modrinthId = property("publish.modrinth").toString()
    val curseforgeId = property("publish.curseforge").toString()
}

class ModDependencies() {
    operator fun get(name: String) = property("deps.$name").toString()
    fun checkSpecified(depName: String): Boolean {
        val property = findProperty("deps.$depName")
        return property != null && property != "[VERSIONED]"
    }
    fun modrinthLocalRuntime(id: String) {
        if (deps.checkSpecified(id)) {
            dependencies.modLocalRuntime("maven.modrinth:$id:${deps[id]}")
        }
    }
}

val mod = ModData()
val env = Environment()
val deps = ModDependencies()
val scVersion = stonecutter.current.version

version = "${mod.version}+${env.title}"
group = mod.group
base { archivesName.set(mod.id) }
//endregion

loom {
    splitEnvironmentSourceSets()

    mods {
        create("template") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }

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

fletchingTable {
    mixins.create("main") {
        default = "${mod.id}.mixins.json"
    }
    mixins.create("client") {
        default = "${mod.id}.client.mixins.json"
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
    minecraft("com.mojang:minecraft:$scVersion")
    mappings("net.fabricmc:yarn:$scVersion+build.${deps["fabric_yarn"]}:v2")
    modImplementation("net.fabricmc:fabric-loader:${deps["fabric_loader"]}")
    if(deps.checkSpecified("fabric_api"))
        modImplementation("net.fabricmc.fabric-api:fabric-api:${deps["fabric_api"]}")

    deps.modrinthLocalRuntime("fungible-updated")
    deps.modrinthLocalRuntime("modmenu")
}

//region Building
java {
    withSourcesJar()
    val java = if (stonecutter.eval(scVersion, ">=1.20.6")) JavaVersion.VERSION_21 else JavaVersion.VERSION_17
    targetCompatibility = java
    sourceCompatibility = java
}

tasks.processResources {
    inputs.property("version", mod.version)
    inputs.property("id", mod.id)
    inputs.property("name", mod.name)
    inputs.property("range", env.range)
    inputs.property("description", mod.description)
    inputs.property("homepage", mod.homepage)
    inputs.property("repository", mod.repository)
    inputs.property("discord", mod.discord)
    inputs.property("range", env.range)

    val map = mapOf(
        "version" to mod.version,
        "id" to mod.id,
        "name" to mod.name,
        "range" to env.range,
        "description" to mod.description,
        "homepage" to mod.homepage,
        "repository" to mod.repository,
        "discord" to mod.discord,
        "range" to env.range,
    )

    filesMatching("fabric.mod.json") { expand(map) }
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    from(tasks.remapJar.get().archiveFile)
    into(rootProject.layout.buildDirectory.file("libs/${mod.version}"))
    dependsOn("build")
}
//endregion

//region Publishing
publishMods {
    file = tasks.remapJar.get().archiveFile
    additionalFiles.from(tasks.remapSourcesJar.get().archiveFile)
    displayName = "${mod.name} ${mod.version} for ${env.title}"
    version = mod.version
    changelog = rootProject.file("CHANGELOG.md").readText()
    type = env.channel
    modLoaders.add("fabric")

    dryRun = env.dry_run
            || (env.modrinthId != "..." && providers.environmentVariable("MODRINTH_TOKEN").getOrNull() == null)
            || (env.curseforgeId != "..." && providers.environmentVariable("CURSEFORGE_TOKEN").getOrNull() == null)

    modrinth {
        projectId = property("publish.modrinth").toString()
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.addAll(env.targets)
        requires {
            slug = "fabric-api"
        }
    }

//    Uncomment publishing order in stonecutter.gradle.kts too if you want to publish to Curseforge
//    curseforge {
//        projectId = property("publish.curseforge").toString()
//        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
//        minecraftVersions.addAll(mod.targets)
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
            version = scVersion

            from(components["java"])
        }
    }
}
//endregion