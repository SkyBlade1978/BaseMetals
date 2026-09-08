[![Discord](https://img.shields.io/badge/Discord-MMD-green.svg?style=flat&logo=Discord)](https://discord.moddev.zone)
[![CurseForge downloads](https://cf.way2muchnoise.eu/full_base-metals_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/base-metals)
[![Supported Minecraft versions](https://cf.way2muchnoise.eu/versions/Minecraft_base-metals_all.svg)](https://www.curseforge.com/minecraft/mc-mods/base-metals)
[![Build, test, and audit](https://github.com/MinecraftModDevelopmentMods/BaseMetals/actions/workflows/ci.yml/badge.svg?branch=master-1.18)](https://github.com/MinecraftModDevelopmentMods/BaseMetals/actions/workflows/ci.yml?query=branch%3Amaster-1.18)

# Base Metals

Base Metals adds historically common metals, useful alloys, and fantasy metals
to Minecraft. Depending on the material, these are available as ores, storage
and decorative blocks, molten fluids, crafting components, tools, armour,
shields, bows, crossbows, arrows, and bolts. The mod also includes Crack
Hammers, scythes, metal anvils, the human detector, villager trades,
advancements, and structure loot.

The Minecraft 1.18.2 line is a native Forge/Java 17 port. Its current version is
`3.0.1.118021`: functional version `3.0.1`, built for the `118021` target
(Minecraft 1.18.2, Forge). It preserves the historical `basemetals` registry
IDs while replacing MMDLib, Additional Loot Tables, and the old Base Metals
world generator with modern Forge and data-pack systems.

## Requirements

- Minecraft `1.18.2`
- Forge `40.3.0` or newer in the Minecraft 1.18.2 Forge 40 line
- Java 17
- OreSpawn `[4.0.1,5.0.0)` on both client and server

This version is built and qualified against OreSpawn `4.0.16.118021`. Put
`BaseMetals-3.0.1.118021.jar` and a compatible OreSpawn JAR in the `mods`
directory. MMDLib and Additional Loot Tables must not be carried forward from
an older installation. Mineralogy is optional.

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
- Supported ore powders can be smelted into ingots, and alloy blends provide the
  historical furnace-based alloying route.
- Scythes harvest a horizontal 3x3 area while respecting normal Forge block
  break and protection events, drops, enchantments, and durability.
- Material shields can be upgraded at an anvil with one strictly harder tagged
  plate while retaining their enchantments.
- Base Metals bows fire its material arrows. Its legacy-style crossbows are
  draw-and-release weapons which fire material bolts.
- Configurable armour and melee effects give Adamantine, Aquarium, Cold Iron,
  Lead, Mithril, and Starsteel their distinctive behaviour. Held Starsteel
  equipment repairs by one durability every 200 server ticks; Starsteel armour
  does not regenerate.
- Molten-metal buckets place and collect the corresponding fluid blocks. These
  fluids are world blocks and compatibility resources rather than a second
  Base Metals processing system.

The common configuration deliberately contains only four default-enabled
switches: special equipment effects, Starsteel regeneration, mercury immersion
effects, and villager trades. Registries, materials, recipes, and world
generation are stable data contracts and cannot be disabled individually by
the common configuration.

## Ore generation

[OreSpawn](https://www.curseforge.com/minecraft/mc-mods/mmd-orespawn) is the
only ore-placement engine used by this port. Base Metals contains no native or
fallback world generator.

Fresh installations enable ten ore rules. Cold Iron and Adamantine generate in
the Nether, Starsteel generates in the End, and the remaining enabled ores use
explicit 1.18 height ranges in ordinary dimensions other than the Nether and
End. Vanilla copper generation remains enabled. The Base Metals Copper,
Antimony, and Bismuth ore blocks remain registered for compatibility and data
packs, but do not generate by default.

Base Metals does not add rock strata. If Mineralogy is installed, OreSpawn uses
the same Base Metals rules and its declared rock-family host tags, avoiding a
second copy of each ore.

See [World generation](docs/WORLDGEN.md) for the complete provider contract,
default distributions, and data-pack override guidance.

## Updating an old world

The port supports direct upgrades from Base Metals 1.10 and 1.12, including the
original Cyano 2.4 line and the later MMD 2.5 line. The first 1.18 launch makes
world-local backups before converting legacy numeric chunks, registry aliases,
inventories, equipment, block entities, fluids, and OreSpawn 3 configuration.
Migration stops instead of guessing when required legacy registry information
is unavailable.

Always perform the first upgrade on a copy of the world and keep the untouched
original. Read [Migration](docs/MIGRATION.md) before starting an upgraded world.

## Mod and data-pack compatibility

Forge item, block, and fluid tags are Base Metals' public compatibility API.
Conditional data is included for:

- Mekanism 10.2
- Thermal Expansion 9
- Tinkers' Construct 3.7
- Ender IO 6 alpha

See [Compatibility](docs/COMPATIBILITY.md) for the supported processing chains,
tag aliases, copper rules, and intentionally retired legacy integrations. See
[Supported versions](docs/VERSIONS.md) for the exact dependency and Maven
contracts.

## Building and contributing

Base Metals uses ForgeGradle `7.0.34`, Gradle `9.6.1`, official Minecraft
mappings, and the Java 17 toolchain. A normal command-line build resolves the
qualified OreSpawn release from CurseMaven:

```text
./gradlew clean check build javadoc
```

Generate and verify Eclipse launches with:

```text
./gradlew genEclipseRuns eclipse isolateEclipseProductionRuns verifyEclipseProductionClasspath
```

The CI workflow also runs all required GameTests, generated-data drift checks,
packaged client and dedicated-server smokes, release-JAR audits, dependency
identity checks, and a second clean build to prove byte-identical release
artifacts. GameTests, smoke probes, migration fixtures, and reference sources
are isolated from the published JAR.

Release artifacts use Maven coordinate
`zone.moddev.mc.basemetals:BaseMetals:3.0.1.118021`.

Report defects and compatibility problems through the
[Base Metals issue tracker](https://github.com/MinecraftModDevelopmentMods/BaseMetals/issues).

Base Metals is licensed under [LGPL-2.1](LICENSE). Bundled or derived third-party
art is covered by the accompanying attribution and licence files.
