# Base Metals 3.0.0

Base Metals 3.0.0 is the Minecraft 1.18.2/Forge 40 port of Base Metals. It adds
22 metals and alloys, their blocks and equipment, molten fluids, the Crack
Hammer, scythes, material shields, bows, bolt-launching crossbows, trades,
advancements, and structure loot.

This branch is a native Java 17 mod. It does not contain or require MMDLib,
Additional Loot Tables, Mineralogy, or a legacy ore generator.

## Requirements

- Minecraft 1.18.2
- Forge 40.3.0 or newer in the Forge 40 line
- OreSpawn 4.0.1 or newer, but lower than 5.0.0

Put `BaseMetals-1.18.2-3.0.0.jar` and a compatible OreSpawn JAR in the `mods`
directory on both client and server.

## World generation

OreSpawn is Base Metals' only ore-placement engine. Fresh installations leave
vanilla copper generation enabled and do not generate Base Metals copper,
antimony, or bismuth ore. The other ten configured ores use explicit 1.18
height ranges and dimension selectors. Base Metals provides no rock strata;
Mineralogy is optional and its rock-family tags are used automatically when it
is installed.

See [docs/WORLDGEN.md](docs/WORLDGEN.md) for the complete provider contract and
[docs/MIGRATION.md](docs/MIGRATION.md) before upgrading an existing world.

Direct Base Metals 1.12 worlds are pre-flattened before Forge loads their
chunks. Original region files are retained in a world-local backup, and the
migrator fails closed if it encounters an unknown third-party numeric block
ID. Always perform the first start on a copy.

## Gameplay

- Crack Hammers apply `basemetals:crushing` recipes to mined blocks. Right-click
  below dropped items to crush one item, or sneak to crush as much of the stack
  as the hammer's remaining durability permits.
- Scythes harvest a server-authoritative horizontal 3x3 area and route every
  neighbour through Forge's normal block-break and protection events.
- A material shield can be upgraded in an anvil with one strictly harder plate.
- Base Metals bows accept Base Metals arrows; the legacy-style crossbows draw
  and release Base Metals bolts.
- Held Starsteel equipment repairs by one durability every 200 server ticks.
  Starsteel armour intentionally does not regenerate.

The common configuration contains only four default-true switches: special
equipment effects, Starsteel regeneration, mercury immersion effects, and
villager trades. Registries, recipes, materials, and world generation are
data-pack contracts and are not configuration-gated.

## Data-pack compatibility

Forge item, block, and fluid tags are the public compatibility API. Conditional
data is included for Mekanism 10.2, Thermal Expansion 9, Tinkers' Construct 3.7,
and Ender IO 6 alpha. See [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md).

## Development

Use Java 17 and the workspace Gradle cache. The complete verification and data
generation workflow is documented in [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).

Base Metals is licensed under LGPL-2.1.
