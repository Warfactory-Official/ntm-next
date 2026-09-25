extra["buildRevision"] = runCatching {
    val git = providers.exec {
        workingDir = rootDir
        commandLine("git", "describe", "--always", "--abbrev=8", "--dirty", "--exclude", "*")
        isIgnoreExitValue = true
    }
    if (git.result.get().exitValue == 0) git.standardOutput.asText.get().trim() else null
}.getOrNull().orEmpty().ifEmpty { "unknown" }

extra["publishVersion"] = "${property("mod_version")}+mc${property("minecraft_version")}" +
        if (hasProperty("build.release")) "" else "-SNAPSHOT"

subprojects {
    layout.buildDirectory.set(rootProject.layout.projectDirectory.dir("build/" + name))
}
