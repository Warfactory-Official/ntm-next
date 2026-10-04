# NTM Next

HBM's Nuclear Tech Mod for Minecraft 26.2, with Fabric and NeoForge builds.

## Build

Requires JDK 25.

```sh
./gradlew build
```

## Gradle Kotlin DSL

Replace `<published-version>` with the version you use. For Fabric:

```kotlin
val ntmVersion = "<published-version>"

repositories {
    maven("https://repo.warfactory.co/releases")
}

dependencies {
    compileOnly("com.hbm:ntm-next-fabric:$ntmVersion")
}
```

For NeoForge:

```kotlin
val ntmVersion = "<published-version>"

repositories {
    maven("https://repo.warfactory.co/releases")
}

dependencies {
    compileOnly("com.hbm:ntm-next-neoforge:$ntmVersion")
}
```

Snapshots use `https://repo.warfactory.co/snapshots` and versions ending in `-SNAPSHOT`.
