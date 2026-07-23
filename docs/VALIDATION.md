# Base Metals 3.0.0 validation

This page records the deterministic validation completed for the Minecraft
1.18.2 port. It separates current-code verification from historical fixture
evidence so release claims remain reproducible.

## 1.12 functionality audit

The July 2026 re-audit compared the retained 1.12 implementation, its generated
runtime data, and the MMDLib behavior Base Metals actually used against the
1.18.2 implementation.

- All 1,131 functional 1.12 recipe JSON files map to a 1.18 recipe. After
  normalizing Forge tag names, there are no missing recipes and no pattern,
  result-item, or result-count differences.
- The remaining 18 ingredient differences are deliberate: nine alloy
  small-blend separation recipes, four Base Metals-owned copper
  compacting/decompacting loops, and five Obsidian block inputs represented by
  `forge:storage_blocks/obsidian`.
- Registries, material formulas, harvest tiers, fuels, crafting and furnace
  recipes, recycling, crackhammers, scythes, shields, bows, legacy crossbows,
  projectiles, special armor/tool effects, Starsteel repair, trades, tooltips,
  advancements, block behavior, loot, fluids, and missing mappings have
  automated contract coverage.
- Generated resources have deterministic ordering and are checked for drift.

## Current automated gates

The following commands pass with Java 17 and
`D:\BaseMetals\.gradle-verify-cache`:

```powershell
.\gradlew test
.\gradlew verifyGeneratedData build genEclipseRuns eclipse
.\gradlew runGameTestServer
```

The unit/resource suite contains 28 tests. The Forge server suite passes all 25
required GameTests. The latter includes real recipe loading and furnace use,
crackhammer and scythe behavior, equipment effects, shield repair/upgrades,
ammunition and projectile persistence, fluids, advancements, and compatibility
recipe counts.

Each supported compatibility profile passes the same 25-test server suite
independently, and the complete set passes together:

```powershell
.\gradlew runGameTestServer -PcompatSmoke=mekanism
.\gradlew runGameTestServer -PcompatSmoke=thermal
.\gradlew runGameTestServer -PcompatSmoke=tconstruct
.\gradlew runGameTestServer -PcompatSmoke=enderio
.\gradlew runGameTestServer -PcompatSmoke=all
```

These runs cover 130 Thermal, 472 Tinkers' Construct, and 44 Ender IO recipes,
plus Mekanism's eight historical processing families and registered
chemicals/slurries.

OreSpawn 4.0.1 passes:

```powershell
.\gradlew test processResources build javadoc genEclipseRuns eclipse
```

An authoritative server bake reports the Base Metals revision-1 provider
active and bakes 11 ore definitions across the applicable dimension selector
tables. Nether and End rules can be rejected by provisional pre-server bakes
because those registries do not yet exist; these diagnostics are DEBUG-only.
The authoritative server bake succeeds without a placement warning.

## Runtime smokes

- A fresh bounded client smoke created and joined
  `basemetals-integrated-smoke-20260723`, ticked the integrated server for 208
  ticks, saved all three vanilla dimensions, and shut down cleanly.
- The captured upgraded 1.12 fixture was re-read with the current code. All
  6,851 migrated block-state placements and 1,038 saved item stacks matched.
- The fixture's original conversion marker records 648 converted legacy chunks
  in four region files, with a recoverable pre-conversion region backup.

The upgraded-world rerun validates current read compatibility. It is not a
second pristine direct conversion; the preserved marker and backup are the
evidence for the original direct conversion.

## Release archive audit

`BaseMetals-1.18.2-3.0.0.jar` contains the schema-3, revision-1 OreSpawn
provider and requires OreSpawn `[4.0.1,5.0.0)`. The archive contains no MMDLib,
Mineralogy, native/fallback generator, removed integration classes,
`forge_marker`, recipes under `assets`, or plural legacy texture paths.

## Separate performance gate

The five-repetition, 289-fresh-chunk benchmark and its JFR allocation inspection
are a separate release-performance gate. They were not rerun as part of this
functionality audit and must not be inferred from the results above.
