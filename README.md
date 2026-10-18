<h1 align="center"> NTM NEXT <br>
	<a href="https://www.curseforge.com/minecraft/mc-mods/ntm-next"><img src="http://cf.way2muchnoise.eu/1699386.svg" alt="CF"></a>
	<a href="https://discord.gg/eKFrH7P5ZR"><img src="https://img.shields.io/discord/1241479482964054057?color=5865f2&label=Discord&style=flat" alt="Discord"></a>
    <br>
</h1>
HBM's Nuclear Tech Mod for Minecraft 26.2, with Fabric and NeoForge builds.

## Licensing

NTM NEXT is free software, available under the GNU Lesser General Public License, version 3 (the license). Binaries,
code, and assets credited to movblock are All Rights Reserved, except where expressly granted by the license. Assets
and code derived from HBM's Nuclear Tech Mod keep their original authors' credit. The mod icon `hbm.png` is All Rights
Reserved and is not covered by the license.

If you distribute a port of NTM NEXT to another version, or reuse its code or assets in your own work, read the
license first. It requires you to:

- keep every copyright and license notice intact: the SPDX headers in source files, `REUSE.toml` and
  `THIRD_PARTY_NOTICES`;
- state prominently that you modified the work, with a date;
- license your work under LGPL-3.0-only or GPL-3.0-only;
- make the complete corresponding source available for every build you distribute.

Addons that only depend on the published jars, without copying NTM NEXT code or assets, are not modified versions (the license §0).

Breaking these terms ends your rights under the license (GPLv3 §8). I (movblock) intend to exercise my full rights against any
copy that does, including but not limited to DMCA takedown notices to the relevant distribution platforms.

### Additional terms

Under GPLv3 §7(c) and §7(e), these additional terms apply to material authored by movblock:

- the license grants no rights to the name "NTM NEXT";
- a modified version must not be named NTM NEXT or presented as the official release, and must be marked as
  different from it.
  
Accurately identifying a work as derived from NTM NEXT does not violate these additional terms, provided it is clearly distinguished from the official release and does not imply endorsement. Ports are encouraged to acknowledge NTM NEXT as their upstream project and provide a link to it.

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
