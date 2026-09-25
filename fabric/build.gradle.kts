plugins {
    java
    `maven-publish`
    id("net.fabricmc.fabric-loom") version ("1.18.2")
}

version = property("mod_version")!!
group = property("mod_group_id")!!
val implementationVersion = "$version+${rootProject.extra["buildRevision"]}"

base {
    archivesName = "${property("mod_id")}-fabric"
}

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    includeEmptyDirs = false
    from(sourceSets.main.get().allJava)
}

publishing {
    publications {
        create<MavenPublication>("mavenJar") {
            groupId = property("mod_group_id") as String
            artifactId = "ntm-next-fabric"
            version = rootProject.extra["publishVersion"] as String
            artifact(tasks.named("jar"))
            artifact(sourcesJar)
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

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED")
    options.compilerArgs.add("--add-modules=jdk.incubator.vector")
    options.forkOptions.memoryMaximumSize = "2g"
}

sourceSets.main {
    resources.exclude("assets/**/*.obj")
}

val javacInternalExports = listOf("api", "code", "comp", "main", "model", "parser", "tree", "util")
    .map { "--add-exports=jdk.compiler/com.sun.tools.javac.$it=ALL-UNNAMED" }
val javacInternalOpens = listOf("main")
    .map { "--add-opens=jdk.compiler/com.sun.tools.javac.$it=ALL-UNNAMED" }

tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs.addAll(listOf(
        "-Xplugin:tenon-asm",
        "-Xplugin:tenon-inject packages=com.hbm.interfaces.injected",
        "-Xplugin:tenon-traits templates=" +
                layout.projectDirectory.dir("src/main/templates/java/com/hbm/tileentity").asFile.absolutePath,
        "-Xplugin:hbm-compiler syncTo=com/hbm/inventory/container-sync.idx"
    ))
    options.compilerArgs.addAll(javacInternalExports)
    options.isFork = true
    options.forkOptions.jvmArgs!!.addAll(javacInternalExports)
    options.forkOptions.jvmArgs!!.addAll(javacInternalOpens)
}

val nativeResources = configurations.create("nativeResources") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val nativePlatforms = providers.gradleProperty("hbm.native.platforms").getOrElse("host")
require(nativePlatforms in setOf("host", "all")) { "hbm.native.platforms must be host or all" }
dependencies.add(
    nativeResources.name, dependencies.project(
        mapOf(
            "path" to ":native",
            "configuration" to if (nativePlatforms == "all") "ciResources" else "hostResources",
        )
    )
)

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

val clientDependencyJar = tasks.register<Jar>("clientDependencyJar") {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    archiveFileName.set("hbm-client-dependency.jar")
    destinationDirectory.set(layout.buildDirectory.dir("clientDependency"))
    inputs.property("version", version)
    from("src/main/clientDependency") { expand("version" to version) }
}

tasks.named<ProcessResources>("processResources") {
    from(nativeResources)
    from(clientDependencyJar) { into("META-INF/jars") }
    filesMatching("fabric.mod.json") { expand("version" to version) }
}

repositories {
    mavenCentral()
    maven("https://maven.blamejared.com/")
    maven("https://modmaven.dev")
    maven("https://repo.warfactory.co/releases") {
        name = "Warfactory"
        content { includeGroup("dev.engine_room") }
    }
    maven("https://repo.warfactory.co/snapshots") {
        content { includeGroup("mov.movblock.tenon") }
    }
    maven("https://api.modrinth.com/maven") {
        name = "Modrinth"
        content { includeGroup("maven.modrinth") }
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

dependencies {

    compileOnly("cc.tweaked:cc-tweaked-26.2-fabric-api:1.120.2")
    compileOnly("maven.modrinth:create-fly:26.2-rc-2-6.0.9-1")
    compileOnly("maven.modrinth:techreborn:YsPVZIsK")
    compileOnly("maven.modrinth:reborncore:xiERLsCF")
    compileOnly("maven.modrinth:compact-storage:mehMI8pF")
    compileOnly("maven.modrinth:travelersbackpack:jp51VM8M")
    compileOnly("maven.modrinth:balm:LcKC6Cxw")
    compileOnly("maven.modrinth:trinkets-updated:jhbhwQFz")
    compileOnly("maven.modrinth:iris:gxZWWnKH")
    compileOnly("maven.modrinth:sodium:xJZxADzI")
    compileOnly("io.github.douira:glsl-transformer:3.0.0-pre3")

    compileOnly("org.antlr:antlr4-runtime:4.13.1")
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")

    compileOnly("io.github.llamalad7:mixinextras-common:0.5.5")

    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("maven.modrinth:scalablelux:0.3.0-alpha.0.3+26.2")
    compileOnly("maven.modrinth:lithium:mc26.2-0.25.3-fabric")
    compileOnly("mov.movblock.tenon:tenon-strip-api:${property("tenon_version")}")

    annotationProcessor(project(":processor"))
    annotationProcessor("mov.movblock.tenon:tenon-asm:${property("tenon_version")}")
    annotationProcessor("mov.movblock.tenon:tenon-traits:${property("tenon_version")}")
    annotationProcessor("mov.movblock.tenon:tenon-inject:${property("tenon_version")}")

    implementation("dev.engine_room:crankshaft-fabric:1.5.3+mc26.2")

    compileOnly("mezz.jei:jei-26.2-common-api:30.35.0.224")

    compileOnly("me.shedaniel:RoughlyEnoughItems-api:${property("rei_version")}")
    compileOnly("me.shedaniel:RoughlyEnoughItems-default-plugin:${property("rei_version")}")

    compileOnly("maven.modrinth:jade:26.2.11+fabric")
    compileOnly("mcp.mobius.waila:wthit-api:fabric-20.0.0")

    compileOnly("maven.modrinth:continuity:3.0.1+26.2")

    implementation("teamreborn:energy:5.0.0")
}

loom {
    accessWidenerPath = file("src/main/resources/hbm.classtweaker")

    mixin {
        useLegacyMixinAp = false
    }

}
