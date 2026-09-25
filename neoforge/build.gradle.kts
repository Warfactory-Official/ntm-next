plugins {
    java
    `maven-publish`
    id("net.neoforged.moddev") version "2.0.147"
}

version = property("mod_version")!!
group = property("mod_group_id")!!

base {
    archivesName = "${property("mod_id")}-neoforge"
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
            artifactId = "ntm-next-neoforge"
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

tasks.named<ProcessResources>("processResources") {
    from(nativeResources)
}

tasks.named<Jar>("jar") {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    manifest.attributes("Implementation-Version" to "$version+${rootProject.extra["buildRevision"]}")
    includeEmptyDirs = false
    exclude("assets/**/*.obj")
}

repositories {
    maven("https://maven.caffeinemc.net/releases") {
        content { includeGroup("net.caffeinemc") }
    }
    mavenCentral()
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.blamejared.com/")
    maven("https://repo.warfactory.co/releases") {
        name = "Warfactory"
        content { includeGroup("dev.engine_room") }
    }
    maven("https://repo.warfactory.co/snapshots") {
        content { includeGroup("mov.movblock.tenon") }
    }
    maven("https://api.modrinth.com/maven") {
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

dependencies {
    compileOnly("cc.tweaked:cc-tweaked-26.2-forge-api:1.120.2")
    compileOnly("maven.modrinth:sophisticated-core:HJLJBWYT")
    compileOnly("maven.modrinth:sophisticated-storage:9jOzQdN5")
    compileOnly("maven.modrinth:sophisticated-backpacks:LPnOSNVg")
    compileOnly("maven.modrinth:compact-storage:nIOL2PLd")
    compileOnly("maven.modrinth:travelersbackpack:wxBR2pcr")
    compileOnly("maven.modrinth:balm:lGba52SO")
    compileOnly("maven.modrinth:trinkets-updated:jhbhwQFz")
    compileOnly("maven.modrinth:curios:BI7D0sbK")
    compileOnly("maven.modrinth:iris:k55HdONq")
    compileOnly("net.caffeinemc:sodium-neoforge-mod:0.9.2+mc26.2")
    compileOnly("io.github.douira:glsl-transformer:3.0.0-pre3")

    compileOnly("org.antlr:antlr4-runtime:4.13.1")
    compileOnly("io.github.llamalad7:mixinextras-common:0.5.5")

    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("maven.modrinth:scalablelux:0.3.0-alpha.0.16+26.2")
    compileOnly("maven.modrinth:lithium:mc26.2-0.25.3-neoforge")
    compileOnly("mov.movblock.tenon:tenon-strip-api:${property("tenon_version")}")

    annotationProcessor(project(":processor"))
    annotationProcessor("mov.movblock.tenon:tenon-asm:${property("tenon_version")}")
    annotationProcessor("mov.movblock.tenon:tenon-traits:${property("tenon_version")}")
    annotationProcessor("mov.movblock.tenon:tenon-inject:${property("tenon_version")}")

    implementation("dev.engine_room:crankshaft-neoforge:1.5.3+mc26.2")

    compileOnly("mezz.jei:jei-26.2-common-api:30.35.0.224")

    compileOnly("me.shedaniel:RoughlyEnoughItems-api:${property("rei_version")}")
    compileOnly("me.shedaniel:RoughlyEnoughItems-default-plugin:${property("rei_version")}")
    compileOnly("me.shedaniel:RoughlyEnoughItems-neoforge:${property("rei_version")}") { isTransitive = false }

    compileOnly("maven.modrinth:jade:26.2.10+neoforge")
    compileOnly("mcp.mobius.waila:wthit-api:neo-20.0.0")
    compileOnly("maven.modrinth:the-one-probe:26.2_neo-15.0.1")

    compileOnly("curse.maven:neocontinuity-1377279:8384811")
    compileOnly("maven.modrinth:ctmlib:26.2.0.1")
}

neoForge {
    version = property("neo_version") as String

    interfaceInjectionData.from(file("src/main/resources/interface-injection.json"))

}
