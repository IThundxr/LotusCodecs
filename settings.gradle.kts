pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net")
        maven("https://maven.neoforged.net")
        maven("https://maven.architectury.dev")
        gradlePluginPortal()
    }
}

plugins {
    id("dev.lukebemish.multisource") version "0.2.2"
}

multisource.of(":") {
    configureEach {
        minecraft.add("com.mojang:minecraft:1.21.1")
        mappings.add(loom.officialMojangMappings())
    }
    common("minecraft", listOf()) {}
    fabric("minecraftFabric", listOf("minecraft")) {}
    neoforge("minecraftNeoforge", listOf("minecraft")) {
        neoForge.add("net.neoforged:neoforge:21.1.31")
    }
    repositories {
        removeIf { it.name == "Forge" }
    }
}

rootProject.name = "LotusCodecs"