// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

plugins { base }

val stagedDir = layout.buildDirectory.dir("resources/host")
val osName = System.getProperty("os.name").lowercase()
val hostOs = when {
    osName.startsWith("windows") -> "windows"
    osName.startsWith("mac") || osName.startsWith("darwin") -> "osx"
    osName.startsWith("linux") -> "linux"
    else -> osName.replace(Regex("[^a-z0-9]+"), "")
}
val hostArch = when (val arch = System.getProperty("os.arch").lowercase()) {
    "x86_64", "amd64", "x64" -> "x86_64"
    "aarch64", "arm64" -> "aarch_64"
    else -> arch
}
val builtLibName = System.mapLibraryName("ntm_next_native").removePrefix("lib")
val artifactName = "$hostOs-$hostArch-" +
        System.mapLibraryName("ntm_next_native")

fun exeName(base: String) = if (System.getProperty("os.name").startsWith("Windows")) "$base.exe" else base

fun requireTool(property: String, fallback: String, home: String?): String {
    val explicit = providers.gradleProperty(property).orNull
    if (explicit != null) return explicit
    if (home != null) {
        val candidate = File(home, "bin/${exeName(fallback)}")
        if (!candidate.isFile) throw GradleException("$home has no bin/${exeName(fallback)}")
        return candidate.absolutePath
    }
    return exeName(fallback)
}

val cxx = requireTool("hbm.native.cxx", "g++", providers.gradleProperty("hbm.mingw.home").orNull)
val cc = requireTool("hbm.native.cc", "gcc", providers.gradleProperty("hbm.mingw.home").orNull)

val buildDirFor = layout.buildDirectory.dir("cmake/$hostOs-$hostArch")

val configure = tasks.register<Exec>("configure") {
    group = "native"
    description = "Configures the merged native library."
    inputs.dir(layout.projectDirectory.dir("src"))
    inputs.file(layout.projectDirectory.file("CMakeLists.txt"))
    inputs.property("cc", cc)
    inputs.property("cxx", cxx)

    outputs.file(buildDirFor.map { it.file("CMakeCache.txt") })
    outputs.file(buildDirFor.map { it.file("build.ninja") })
    commandLine(
        "cmake", "-S", projectDir.absolutePath, "-B", buildDirFor.get().asFile.absolutePath,
        "-G", "Ninja", "-DCMAKE_BUILD_TYPE=Release",
        "-DCMAKE_CXX_COMPILER=$cxx", "-DCMAKE_C_COMPILER=$cc",
    )
}

val compileNative = tasks.register<Exec>("compileNative") {
    group = "native"
    description = "Compiles the merged native library for the host platform."
    dependsOn(configure)
    inputs.dir(layout.projectDirectory.dir("src"))
    inputs.file(layout.projectDirectory.file("CMakeLists.txt"))
    inputs.property("cc", cc)
    inputs.property("cxx", cxx)
    outputs.file(buildDirFor.map { it.file(builtLibName) })
    commandLine("cmake", "--build", buildDirFor.get().asFile.absolutePath)
    doLast {
        val library = buildDirFor.get().file(builtLibName).asFile
        check(library.isFile) { "CMake produced no native library at $library" }
    }
}

val stage = tasks.register<Sync>("stage") {
    group = "native"
    description = "Prepares host native resources under the build directory."
    from(files(buildDirFor.map { it.file(builtLibName) }).builtBy(compileNative)) {
        into("ntm_natives")
        rename { artifactName }
    }
    into(stagedDir)
}
tasks.named("assemble") { dependsOn(stage) }

configurations.create("hostResources") {
    isCanBeResolved = false
    isCanBeConsumed = true
    outgoing.artifact(stagedDir) { builtBy(stage) }
}

val ciLinuxCc = providers.environmentVariable("HBM_CI_LINUX_CC")
val ciLinuxCxx = providers.environmentVariable("HBM_CI_LINUX_CXX")
val ciDarwinCc = providers.environmentVariable("HBM_CI_DARWIN_CC").orElse("gcc-16")
val ciDarwinCxx = providers.environmentVariable("HBM_CI_DARWIN_CXX").orElse("g++-16")
val ciNativesDir = layout.buildDirectory.dir("native-supplied")
val ciLinuxDir = layout.buildDirectory.dir("ci/linux")
val ciOsxAarch64Dir = layout.buildDirectory.dir("ci/osx-aarch64")
val ciOsxX86Dir = layout.buildDirectory.dir("ci/osx-x86_64")

fun ciLinuxTool(name: String, tool: Provider<String>): String =
    tool.orNull ?: throw GradleException("HBM_CI_LINUX_${name.uppercase()} is not set")

val ciConfigureLinux = tasks.register<Exec>("ciConfigureLinux") {
    group = "ci"
    inputs.dir(layout.projectDirectory.dir("src"))
    inputs.file(layout.projectDirectory.file("CMakeLists.txt"))
    inputs.property("cc", ciLinuxCc)
    inputs.property("cxx", ciLinuxCxx)
    outputs.file(ciLinuxDir.map { it.file("CMakeCache.txt") })
    outputs.file(ciLinuxDir.map { it.file("build.ninja") })
    doFirst {
        commandLine(
            "cmake", "-S", projectDir.absolutePath, "-B", ciLinuxDir.get().asFile.absolutePath,
            "-G", "Ninja", "-DCMAKE_BUILD_TYPE=Release",
            "-DCMAKE_CXX_COMPILER=${ciLinuxTool("CXX", ciLinuxCxx)}",
            "-DCMAKE_C_COMPILER=${ciLinuxTool("CC", ciLinuxCc)}",
        )
    }
}

val ciBuildLinux = tasks.register<Exec>("ciBuildLinux") {
    group = "ci"
    dependsOn(ciConfigureLinux)
    inputs.dir(layout.projectDirectory.dir("src"))
    inputs.file(layout.projectDirectory.file("CMakeLists.txt"))
    inputs.files(ciConfigureLinux)
    outputs.file(ciLinuxDir.map { it.file("ntm_next_native.so") })
    commandLine("cmake", "--build", ciLinuxDir.get().asFile.absolutePath)
}

fun registerDarwinConfigure(name: String, directory: Provider<Directory>, arch: String, floor: String) =
    tasks.register<Exec>(name) {
        group = "ci"
        inputs.dir(layout.projectDirectory.dir("src"))
        inputs.file(layout.projectDirectory.file("CMakeLists.txt"))
        inputs.property("darwinCc", ciDarwinCc)
        inputs.property("darwinCxx", ciDarwinCxx)
        outputs.file(directory.map { it.file("CMakeCache.txt") })
        outputs.file(directory.map { it.file("build.ninja") })
        doFirst {
            commandLine(
                "cmake", "-S", projectDir.absolutePath, "-B", directory.get().asFile.absolutePath,
                "-G", "Ninja", "-DCMAKE_BUILD_TYPE=Release",
                "-DCMAKE_C_COMPILER=${ciDarwinCc.get()}",
                "-DCMAKE_CXX_COMPILER=${ciDarwinCxx.get()}",
                "-DCMAKE_OSX_ARCHITECTURES=$arch",
                "-DCMAKE_OSX_DEPLOYMENT_TARGET=$floor",
            )
        }
    }

val ciConfigureOsxAarch64 = registerDarwinConfigure("ciConfigureOsxAarch64", ciOsxAarch64Dir, "arm64", "11.0")
val ciConfigureOsxX86 = registerDarwinConfigure("ciConfigureOsxX86", ciOsxX86Dir, "x86_64", "10.15")

val ciBuildOsxAarch64 = tasks.register<Exec>("ciBuildOsxAarch64") {
    group = "ci"
    dependsOn(ciConfigureOsxAarch64)
    inputs.dir(layout.projectDirectory.dir("src"))
    inputs.file(layout.projectDirectory.file("CMakeLists.txt"))
    inputs.files(ciConfigureOsxAarch64)
    outputs.file(ciOsxAarch64Dir.map { it.file("ntm_next_native.dylib") })
    commandLine("cmake", "--build", ciOsxAarch64Dir.get().asFile.absolutePath)
}

val ciBuildOsxX86 = tasks.register<Exec>("ciBuildOsxX86") {
    group = "ci"
    dependsOn(ciConfigureOsxX86)
    inputs.dir(layout.projectDirectory.dir("src"))
    inputs.file(layout.projectDirectory.file("CMakeLists.txt"))
    inputs.files(ciConfigureOsxX86)
    outputs.file(ciOsxX86Dir.map { it.file("ntm_next_native.dylib") })
    commandLine("cmake", "--build", ciOsxX86Dir.get().asFile.absolutePath)
}

val validateCiResources = tasks.register("validateCiResources") {
    group = "ci"
    inputs.files(ciNativesDir)
    doLast {
        val expected = setOf("windows-x86_64-ntm_next_native.dll", "linux-x86_64-libntm_next_native.so",
            "osx-aarch_64-libntm_next_native.dylib", "osx-x86_64-libntm_next_native.dylib")
        val present = ciNativesDir.get().asFile.listFiles().orEmpty()
            .filter { it.isFile && it.length() > 0 }.map { it.name }.toSet()
        check(present == expected) { "Incomplete CI natives: missing=${expected - present}, extra=${present - expected}" }
    }
}

val ciResources = tasks.register<Sync>("ciResources") {
    group = "ci"
    description = "Collects the four shipping native artifacts the per-OS CI jobs built."
    dependsOn(validateCiResources)
    into(layout.buildDirectory.dir("resources/ci"))
    from(ciNativesDir) { into("ntm_natives") }
}

val ciUploadDir = rootProject.layout.buildDirectory.dir("ci-natives")

fun registerCiStage(name: String, built: TaskProvider<Exec>, shipped: String) =
    tasks.register<Copy>(name) {
        group = "ci"
        from(built) { rename { shipped } }
        into(ciUploadDir)
    }

registerCiStage("ciStageLinux", ciBuildLinux, "linux-x86_64-libntm_next_native.so")
registerCiStage("ciStageOsxAarch64", ciBuildOsxAarch64, "osx-aarch_64-libntm_next_native.dylib")
registerCiStage("ciStageOsxX86", ciBuildOsxX86, "osx-x86_64-libntm_next_native.dylib")

configurations.create("ciResources") {
    isCanBeResolved = false
    isCanBeConsumed = true
    outgoing.artifact(layout.buildDirectory.dir("resources/ci")) { builtBy(ciResources) }
}
