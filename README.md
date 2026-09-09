[![Discord](https://img.shields.io/badge/Discord-MMD-green.svg?style=flat&logo=Discord)](https://discord.moddev.zone)
[![CurseForge downloads](https://cf.way2muchnoise.eu/full_base-metals_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/base-metals)
[![Supported Minecraft versions](https://cf.way2muchnoise.eu/versions/Minecraft_base-metals_all.svg)](https://www.curseforge.com/minecraft/mc-mods/base-metals)
[![Build, test, and audit](https://github.com/MinecraftModDevelopmentMods/BaseMetals/actions/workflows/ci.yml/badge.svg?branch=master-1.13.2)](https://github.com/MinecraftModDevelopmentMods/BaseMetals/actions/workflows/ci.yml?query=branch%3Amaster-1.13.2)

# Base Metals

Base Metals adds historically common metals, useful alloys, and fantasy metals
to Minecraft. Depending on the material, these are available as ores, storage
and decorative blocks, molten fluids, crafting components, tools, armour,
shields, bows, crossbows, arrows, and bolts. The mod also includes Crack
Hammers, scythes, metal anvils, the human detector, villager trades,
advancements, and structure loot.

The Minecraft 1.13.2 line is a native Forge/Java 8 backport of the completed
1.18.2 implementation. Its current version is `3.0.1.113021`: functional
version `3.0.1`, built for the `113021` target (Minecraft 1.13.2, Forge). It
preserves the historical `basemetals` registry IDs while replacing MMDLib,
Additional Loot Tables, and the old Base Metals world generator with native
Forge registries, tags, recipes, loot tables, and advancements.

## Requirements

- Minecraft `1.13.2`
- Forge `25.0.223`
- Java 8
- OreSpawn `[4.0.16.113021,5.0.0)` on both client and server

Put `BaseMetals-3.0.1.113021.jar` and OreSpawn `4.0.16.113021` (or a later
compatible OreSpawn 4 build for 1.13.2) in the `mods` directory. MMDLib and
Additional Loot Tables are not dependencies. Mineralogy is optional.

## Materials and gameplay

Base Metals provides 22 materials:

- Natural and industrial metals: Antimony, Bismuth, Copper, Lead, Mercury,
  Nickel, Platinum, Silver, Tin, and Zinc.
- Alloys: Brass, Bronze, Cupronickel, Electrum, Invar, Pewter, and Steel.
- Fantasy materials: Adamantine, Aquarium, Cold Iron, Mithril, and Starsteel.

The available forms intentionally vary by material to preserve the established
Base Metals content. Forge tags are used throughout recipes, so compatible
ores, ingots, powders, plates, rods, gears, and other components from different
mods can interoperate without a Java API dependency.

Notable mechanics include:

- Crack Hammers crush supported mined blocks through the data-driven
  `basemetals:crushing` recipe type. They can also crush dropped items by using
  the hammer on the block beneath them; sneaking processes as much of a stack
  as the hammer's remaining durability allows.
- Supported ore powders can be smelted into ingots, and alloy blends provide
  the historical furnace-based alloying route.
- Scythes harvest a horizontal 3x3 area while respecting Forge block-break and
  protection events, drops, enchantments, and durability.
- Material shields can be upgraded at an anvil with one strictly harder tagged
  plate while retaining their enchantments.
- Base Metals bows fire material arrows. Its legacy-style crossbows are
  draw-and-release weapons which fire material bolts.
- Configurable armour and melee effects give Adamantine, Aquarium, Cold Iron,
  Lead, Mithril, and Starsteel their distinctive behaviour. Held Starsteel
  equipment repairs by one durability every 200 server ticks; its armour does
  not regenerate.
- Dedicated molten-metal buckets place and collect the corresponding fluid
  blocks. They are compatibility resources rather than a second processing
  system.

The common configuration has four default-enabled switches: special equipment
effects, Starsteel regeneration, mercury immersion effects, and villager
trades. Registries, materials, recipes, and world generation remain stable data
contracts and are not individually configuration-gated.

## Ore generation

[OreSpawn](https://www.curseforge.com/minecraft/mc-mods/mmd-orespawn) is the
only ore-placement engine used by this port. Base Metals contains no native or
fallback world generator.

Fresh installations enable all eleven historical ore rules. Cold Iron and
Adamantine generate in the Nether, Starsteel generates in the End, and Copper,
Silver, Tin, Lead, Zinc, Mercury, Nickel, and Platinum generate in ordinary
dimensions. Antimony and Bismuth ore blocks remain registered but do not
generate by default.

Base Metals does not add rock strata. If Mineralogy is installed, OreSpawn uses
the same rules inside its rock families without generating a second copy of
each ore. See [World generation](docs/WORLDGEN.md) for the complete provider
contract.

## Updating an old world

This release is the first Minecraft version after the flattening and supports
direct upgrades from Base Metals 1.10 and 1.12, including the original Cyano
2.4 line and later MMD releases. Its pre-flattening migration hook restores
legacy Base Metals and MMDLib block identities before Mojang converts numeric
chunk data, and also migrates historical item aliases, durability, and universal
fluid buckets.

Always perform the first upgrade on a copy of the world and keep the untouched
original. Read [Migration](docs/MIGRATION.md) before opening an old save. A
world already upgraded to Minecraft 1.18 must not be opened in this older
version.

## Compatibility

Forge item, block, and fluid tags are Base Metals' public compatibility API.
The 1.13.2 line deliberately contains no version-specific Mekanism, Thermal,
Tinkers' Construct, Ender IO, IC2, or Thaumcraft plugin code. Compatible mods
can consume the common tags without linking to Base Metals internals. See
[Compatibility](docs/COMPATIBILITY.md) and [Supported versions](docs/VERSIONS.md).

## Building and contributing

Base Metals uses ForgeGradle `7.0.34` and Gradle `9.6.1`. Gradle runs on Java
17, ForgeGradle's legacy Minecraft transformation utility runs on Java 25, and
all production code compiles and runs on the exact Java 8 toolchain.

```text
./gradlew clean check build javadoc verifyReleaseArtifacts writeReleaseChecksums
```

Generate and verify Eclipse launches with:

```text
./gradlew genEclipseRuns eclipse isolateEclipseProductionRuns verifyEclipseProductionClasspath
```

Release candidates can also be exercised as packaged mods in prepared official
Forge 25 client and server runtimes with `packagedRuntimeIntegrationTest`; pass
their directories through the `packagedForgeClientRuntime` and
`packagedForgeServerRuntime` Gradle properties. These probes load the exact
release JAR and OreSpawn dependency, create real worlds, validate client models
and colours, and remain outside the published artifact.

CI performs an empty-cache Forge bootstrap, contract unit tests, deterministic-data
drift checks, an exact-OreSpawn dedicated-server probe, release-JAR auditing,
checksums, reproducibility checks, CodeQL, wrapper validation, and Eclipse
classpath isolation. Test probes and historical fixtures are excluded from the
published JAR.

Release artifacts use Maven coordinate
`zone.moddev.mc.basemetals:BaseMetals:3.0.1.113021`.

Report defects through the
[Base Metals issue tracker](https://github.com/MinecraftModDevelopmentMods/BaseMetals/issues).

Base Metals is licensed under [LGPL-2.1](LICENSE). Bundled or derived third-party
art is covered by the accompanying attribution and licence files.
