pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        gradlePluginPortal()
    }
}

rootProject.name = "ntm-next"

include("processor")
include("fabric")
include("neoforge")
