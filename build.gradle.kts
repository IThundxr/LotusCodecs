import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

plugins {
    id("java")
    id("maven-publish")
}

val isRelease = System.getenv("RELEASE_BUILD")?.toBoolean() ?: false
val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toInt()

group = "maven_group"()

val build = buildNumber?.let { "-build.${it}" } ?: "-local"
version = "lib_version"() + if (isRelease) "" else build

println("Lotus Codecs: v$version")

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
    
    registerFeature("minecraft") {
        usingSourceSet(sourceSets["minecraft"])
        withSourcesJar()
        capability(project.group as String, "$project.name-minecraft", project.version as String)
        capability(project.group as String, "$project.name-minecraft-common", project.version as String)
    }
    
    registerFeature("minecraftFabric") {
        usingSourceSet(sourceSets["minecraftFabric"])
        capability(project.group as String, "$project.name-minecraft", project.version as String)
        capability(project.group as String, "$project.name-minecraft-fabric", project.version as String)
    }
    
    registerFeature("minecraftNeoforge") {
        usingSourceSet(sourceSets["minecraftNeoforge"])
        capability(project.group as String, "$project.name-minecraft", project.version as String)
        capability(project.group as String, "$project.name-minecraft-neoforge", project.version as String)
    }
}

repositories {
    maven("https://libraries.minecraft.net")
}

dependencies {
    implementation("com.mojang:datafixerupper:7.0.14")
}

tasks.named<Jar>("jar") {
    manifest.attributes("FMLModType" to "LIBRARY")
}

listOf("minecraftJar").forEach { name ->
    tasks.named<Jar>(name) {
        manifest.attributes(mapOf("Implementation-Minecraft-Version" to "minecraft_version"()))
    }
}

tasks.named<dev.lukebemish.multisource.CopyArchiveFileTask>("remapMinecraftFabricJar") {
    archiveFile.set(project.layout.buildDirectory.file("libs/${project.name}-${project.version}-minecraft-intermediary.jar"))
}

tasks.named<dev.lukebemish.multisource.CopyArchiveFileTask>("remapMinecraftNeoforgeJar") {
    archiveFile.set(project.layout.buildDirectory.file("libs/${project.name}-${project.version}-minecraft-neoforge.jar"))
}

listOf("processResources", "processMinecraftResources", "processMinecraftFabricResources", "processMinecraftNeoforgeResources").forEach { name ->
    tasks.named<ProcessResources>(name) {
        val properties = mapOf(
            "version" to project.version,
            "minecraft_version" to "minecraft_version"()
        )

        inputs.properties(properties)

        filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml")) {
            expand(properties)
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "lotus-codecs"
            from(components["java"])
        }
    }
    
    repositories {
        val mavenToken = System.getenv("MAVEN_TOKEN")
        val maven = if (isRelease) "releases" else "snapshots"
        if (mavenToken != null && mavenToken.isNotEmpty()) {
            maven {
                url = uri("https://maven.ithundxr.dev/${maven}")
                credentials {
                    username = "lotus-codecs-github"
                    password = mavenToken
                }
            }
        }
    }
}

operator fun String.invoke(): String {
    return rootProject.ext[this] as? String
        ?: throw IllegalStateException("Property $this is not defined")
}