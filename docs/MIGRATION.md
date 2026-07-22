# Migrating from Base Metals 1.12

Always upgrade a copy and retain the original 1.12 world. Install Base Metals
3.0.0 and OreSpawn 4.0.1, but do not carry MMDLib, Additional Loot Tables, the
OreSpawn 3 plugin, or the old native fallback generator into the 1.18 instance.

Minecraft's normal data fixer cannot identify third-party numeric block IDs
from before the 1.13 flattening. Base Metals therefore pre-flattens Forge 1.12
region chunks before the normal 1.18 loader sees them. It reads the exact
numeric map from an untouched legacy `level.dat` when available, otherwise it
uses the map captured from the public Base Metals 2.5.0-rc2.332 and MMDLib
1.0.0-rc2.36 runtime.

Before replacing any region file, the migrator copies the original to
`basemetals-1.12-region-backup` inside the world. Region replacements are
transactional. A completed scan writes
`BASEMETALS_1_12_BLOCK_MIGRATION_COMPLETE.txt`, recording either the conversion
summary or that no legacy chunks were present; subsequent starts do nothing.
The verified fixture retained 6,851 block-state placements and 1,038 saved
item stacks across the Overworld, Nether, and End.

The packaged map covers the exact public runtime. If a chunk contains a
numeric block ID belonging to some other 1.12 mod, migration stops instead of
guessing or deleting it. Restore the backed-up regions and migrate that mod's
content separately before retrying. External mod states which are present in
the captured map retain their registry name but may fall back to that mod's
default state; Base Metals states retain their old metadata semantics.

Base Metals 3.0.0 preserves historical `basemetals:*` IDs, including hidden
`double_<material>_slab` blocks and molten-fluid block IDs such as
`basemetals:adamantine`. Dedicated bucket items and flowing-fluid registry
entries are new. Missing mappings also forward:

- `basemetals:liquid_mercury` to `basemetals:mercury`;
- `basemetals:carbon_powder` to `basemetals:coal_powder`;
- historical `mmdlib:*` Vanilla Bits blocks, items, and fluids to matching
  `basemetals:*` IDs.

## OreSpawn 3 configuration

OreSpawn 4.0.1 migrates OS3 files before merging provider defaults. When an
installed provider has one rule whose output matches a legacy rule, the old
values are written under that provider rule ID. Explicit user values,
including enabled legacy Base Metals copper, therefore survive without a
duplicate rule. Ambiguous and unmatched outputs retain legacy IDs and are
listed in `config/orespawn-migration/migration-report.txt`.

Fresh profiles use the provider defaults in [WORLDGEN.md](WORLDGEN.md).
Migrated profiles retain explicit enabled flags, quantities, frequencies,
dimensions, and heights.

The generated manifests at `data/basemetals/registry_manifest_1_12.json` and
`data/basemetals/registry_manifest.json` document the runtime baseline,
restored content, and explicit 1.18 additions.
