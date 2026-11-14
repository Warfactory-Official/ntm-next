import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    java
    `maven-publish`
    id("net.fabricmc.fabric-loom") version ("1.19.3")
}

version = property("mod_version")!!
group = property("mod_group_id")!!
val implementationVersion = "$version+${rootProject.extra["buildRevision"]}"

val modMetadata = listOf(
        "minecraft_version", "minecraft_next_version", "fabric_loader_minimum", "fabric_api_version", "crankshaft_version",
        "neoforge_minimum", "neoforge_maximum", "iris_minimum", "sodium_minimum", "lithium_minimum", "lithium_maximum", "ae2_version",
        "crankshaft_fabric_range", "crankshaft_minimum", "sophisticated_core_minimum", "sophisticated_storage_minimum",
        "sophisticated_backpacks_minimum", "ctm_library_version"
    ).associateWith { property(it) as String }

base {
    archivesName = "${property("mod_id")}-fabric"
}

publishing {
    publications {
        create<MavenPublication>("mavenJar") {
            groupId = property("mod_group_id") as String
            artifactId = "ntm-next-fabric"
            version = rootProject.extra["publishVersion"] as String
            artifact(tasks.named("jar"))
        }
    }
    repositories {
        maven {
            name = "warfactory"
            url = uri(
                if (project.hasProperty("build.release")) {
                    "https://repo.warfactory.co/releases"
                } else {
                    "https://repo.warfactory.co/snapshots"
                },
            )
            credentials {
                username = System.getenv("MAVEN_USER")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

val javacInternalExports = listOf("api", "code", "comp", "jvm", "main", "model", "parser", "tree", "util")
    .map { "--add-exports=jdk.compiler/com.sun.tools.javac.$it=ALL-UNNAMED" }
val javacInternalOpens = listOf("main", "comp", "parser", "code", "jvm")
    .map { "--add-opens=jdk.compiler/com.sun.tools.javac.$it=ALL-UNNAMED" }

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED")
    options.compilerArgs.add("--add-modules=jdk.incubator.vector")
    options.compilerArgs.addAll(javacInternalExports)
    options.forkOptions.jvmArgs!!.addAll(
        listOf("-XX:+UnlockExperimentalVMOptions", "-XX:-UseJVMCICompiler", "-XX:-EnableJVMCI")
    )
    options.forkOptions.memoryMaximumSize = "2g"
    options.forkOptions.jvmArgs = (options.forkOptions.jvmArgs ?: emptyList()) + listOf(
        "--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED"
    )
    options.isFork = true
    options.forkOptions.jvmArgs!!.addAll(javacInternalExports)
    options.forkOptions.jvmArgs!!.addAll(javacInternalOpens)
}

sourceSets.main {
    resources.exclude("assets/**/*.obj")
}

tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs.add("-Xplugin:tenon-asm")
}

tasks.named<Jar>("jar") {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    manifest.attributes("Implementation-Version" to implementationVersion)
    inputs.property("implementationVersion", implementationVersion)
    filesMatching("fabric.mod.json") {
        filter { it.replace("\"version\": \"$version\"", "\"version\": \"$implementationVersion\"") }
    }
    includeEmptyDirs = false
    exclude("assets/**/*.obj")
}

val clientDependencyArchive = tasks.register<Jar>("clientDependencyArchive") {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    archiveFileName.set("hbm-client-dependency.jar")
    destinationDirectory.set(layout.buildDirectory.dir("clientDependency/archive"))
    inputs.property("version", version)
    inputs.properties(modMetadata)
    from("src/main/clientDependency") { expand(mapOf("version" to version) + modMetadata) }
}

val clientDependencyJar = publishFile(project, "clientDependencyJar",
    clientDependencyArchive.flatMap { it.archiveFile },
    layout.buildDirectory.file("clientDependency/$version/hbm-client-dependency.jar"))

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", version)
    inputs.properties(modMetadata)
    from(clientDependencyJar) { into("META-INF/jars") }
    filesMatching("fabric.mod.json") { expand(mapOf("version" to version) + modMetadata) }
}

repositories {
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.caffeinemc.net/releases") {
        content { includeGroup("net.caffeinemc") }
    }
    mavenCentral()
    maven("https://maven.blamejared.com/")
    maven("https://modmaven.dev")
    maven("https://repo.warfactory.co/releases") {
        name = "Warfactory"
        content {
            includeGroup("dev.engine_room")
            includeGroup("net.irisshaders")
        }
    }
    maven("https://repo.warfactory.co/snapshots") {
        content { includeGroup("mov.movblock.tenon") }
    }
    maven("https://api.modrinth.com/maven") {
        name = "Modrinth"
        content { includeGroup("maven.modrinth") }
    }
    maven("https://cursemaven.com") {
        content { includeGroup("curse.maven") }
    }
    maven("https://maven.squiddev.cc") {
        content { includeGroup("cc.tweaked") }
    }
    maven("https://maven2.bai.lol") {
        content {
            includeGroup("mcp.mobius.waila")
            includeGroup("lol.bai")
        }
    }
    maven("https://maven.shedaniel.me/") {
        content {
            includeGroup("me.shedaniel")
            includeGroup("me.shedaniel.cloth")
            includeGroup("dev.architectury")
        }
    }
}

val crankshaftVersion = property("crankshaft_version") as String

fun nestedModClasses(
    project: Project, taskName: String, coordinates: String, pattern: String,
    directory: String, fileName: String
): FileCollection {
    val archives = project.configurations.create("${taskName}Archive") {
        isCanBeConsumed = false
        isTransitive = false
    }
    project.dependencies.add(archives.name, coordinates)
    val output = project.layout.buildDirectory.dir("$directory/${coordinates.substringAfterLast(':')}")
    val extracted = project.layout.buildDirectory.dir("$directory/archive")
    val unpack = project.tasks.register<Sync>("${taskName}Archive") {
        from(archives.elements.map { jars -> jars.map { project.zipTree(it.asFile) } }) {
            include(pattern)
            eachFile { path = fileName }
        }
        includeEmptyDirs = false
        into(extracted)
        doLast { check(extracted.get().file(fileName).asFile.isFile) { "No $pattern in $coordinates" } }
    }
    val publish = publishFile(project, taskName,
        unpack.map { _ -> extracted.get().file(fileName) }, output.map { it.file(fileName) })
    return project.files(publish)
}

fun publishFile(
    project: Project, taskName: String, source: Provider<RegularFile>, destination: Provider<RegularFile>
): TaskProvider<Task> = project.tasks.register(taskName) {
    inputs.file(source).withPathSensitivity(PathSensitivity.NONE)
    outputs.file(destination)
    doLast {
        val target = destination.get().asFile.toPath()
        val input = source.get().asFile.toPath()
        if (Files.exists(target) && Files.mismatch(input, target) == -1L) return@doLast
        val pending = target.resolveSibling(target.fileName.toString() + ".tmp")
        Files.createDirectories(target.parent)
        Files.copy(input, pending, StandardCopyOption.REPLACE_EXISTING)
        Files.move(pending, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }
}

val crankshaftIris = nestedModClasses(
    project,
    "crankshaftIris",
    "dev.engine_room:crankshaft-fabric:$crankshaftVersion",
    "META-INF/jars/crankshaft-iris-*.jar",
    "crankshaft-iris",
    "crankshaft-iris.jar"
)

val c2meSerializer = nestedModClasses(
    project,
    "c2meSerializer",
    property("dependency_fabric_c2me") as String,
    "META-INF/jars/c2me-fabric-rewrites-chunk-serializer-*.jar",
    "c2me-serializer",
    "c2me-serializer.jar"
)

dependencies {
    compileOnly(property("dependency_fabric_cc_tweaked_api") as String)
    compileOnly(property("dependency_fabric_alexscaves") as String) { isTransitive = false }
    compileOnly(property("dependency_fabric_codxlib") as String) { isTransitive = false }
    compileOnly(property("dependency_worldedit") as String) { isTransitive = false }
    compileOnly(property("dependency_litematica") as String) { isTransitive = false }
    compileOnly(property("dependency_malilib") as String) { isTransitive = false }
    compileOnly(property("dependency_axiom") as String) { isTransitive = false }
    compileOnly(property("dependency_fabric_effortless_building") as String) { isTransitive = false }
    compileOnly(property("dependency_fabric_create_fly") as String)
    compileOnly(property("dependency_fabric_techreborn") as String)
    compileOnly(property("dependency_fabric_reborncore") as String)
    compileOnly(property("dependency_fabric_compact_storage") as String)
    compileOnly(property("dependency_fabric_travelersbackpack") as String)
    compileOnly(property("dependency_fabric_balm") as String)
    compileOnly(property("dependency_fabric_trinkets_updated") as String)
    compileOnly(property("dependency_fabric_iris") as String)
    compileOnly(property("dependency_fabric_sodium") as String)
    compileOnly("io.github.douira:glsl-transformer:3.0.0-pre3")
    compileOnly("org.antlr:antlr4-runtime:4.13.1")
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")

    compileOnly(property("dependency_fabric_toms_storage") as String) { isTransitive = false }
    compileOnly(property("dependency_fabric_storage_drawers") as String) { isTransitive = false }
    compileOnly(property("dependency_fabric_sound_physics") as String) { isTransitive = false }
    compileOnly(property("dependency_fabric_entity_model_features") as String) { isTransitive = false }
    compileOnly(property("dependency_fabric_punchy_fpa") as String) { isTransitive = false }
    compileOnly(property("dependency_fabric_not_enough_bandwidth") as String) { isTransitive = false }
    compileOnly("com.github.luben:zstd-jni:1.5.6-9")
    compileOnly(property("dependency_fabric_bandwidth_optimizer") as String) { isTransitive = false }
    compileOnly(property("dependency_fabric_flashback") as String) { isTransitive = false }
    compileOnly(property("dependency_fabric_replaymod") as String) { isTransitive = false }
    compileOnly(property("dependency_fabric_hindsight") as String) { isTransitive = false }
    compileOnly("io.github.llamalad7:mixinextras-common:0.5.5")
    compileOnly("org.jspecify:jspecify:1.0.1")
    compileOnly(property("dependency_fabric_scalablelux") as String)
    compileOnly(property("dependency_fabric_lithium") as String)

    annotationProcessor("mov.movblock.tenon:tenon-asm:${property("tenon_version")}")

    implementation("dev.engine_room:crankshaft-fabric:${crankshaftVersion}")
    compileOnly(crankshaftIris)
    compileOnly(c2meSerializer)

    compileOnly(property("dependency_fabric_jei_common_api") as String)

    compileOnly("me.shedaniel:RoughlyEnoughItems-api:${property("rei_version")}")
    compileOnly("me.shedaniel:RoughlyEnoughItems-default-plugin:${property("rei_version")}")

    compileOnly(property("dependency_fabric_jade") as String)
    compileOnly(property("dependency_fabric_wthit_api") as String)

    compileOnly("maven.modrinth:continuity:Jt2sCWTQ")
    compileOnly("maven.modrinth:fusion-connected-textures:wRjdAbXf")

    implementation("teamreborn:energy:5.0.0")
}

loom {
    accessWidenerPath = file("src/main/resources/hbm.classtweaker")

    mixin {
        useLegacyMixinAp = false
    }

}

tasks.named<Jar>("jar") {
    from("../THIRD_PARTY_NOTICES")
    from("../LICENSES") { into("LICENSES") }
    from("../LICENSE") {
        into("LICENSES")
        rename { "GPL-3.0-only.txt" }
    }
}

publishing.publications.named<MavenPublication>("mavenJar") {
    pom {
        licenses {
            license {
                name = "LGPL-3.0-only"
                url = "https://www.gnu.org/licenses/lgpl-3.0.txt"
            }
        }
        scm { url = "https://github.com/Warfactory-Official/ntm-next" }
    }
}
