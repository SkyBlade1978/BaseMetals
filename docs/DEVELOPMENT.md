# Development and verification

The target is Forge 40.3.0, official 1.18.2 mappings, Gradle 8.8, and Java 17.
From the inner checkout use the workspace-local cache:

```powershell
$env:GRADLE_USER_HOME='D:\BaseMetals\.gradle-verify-cache'
$env:JAVA_HOME='C:\Program Files\Java\jdk-17.0.1'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

Build OreSpawn 4.0.1 first, then run Base Metals tasks sequentially:

```powershell
.\gradlew.bat test
.\gradlew.bat runData
.\gradlew.bat processResources compileJava build
.\gradlew.bat runGameTestServer
.\gradlew.bat genEclipseRuns eclipse
```

The generated Eclipse `runClient`, `runServer`, `runData`, and
`runGameTestServer` configurations are direct Java launches. Their
`MOD_CLASSES` entries combine `bin/main` with the stable processed-resource
root at `build/resources/main`. This prevents Eclipse from starting Base
Metals' Java registries without its generated data pack when an unrelated
Gradle dependency check is temporarily unavailable. Rerun the command above
after changing mod metadata or regenerating the workspace.

Generated resources live under `src/generated/resources`. Registrations,
recipes, blockstates, models, tags, loot, advancements, translations, optional
integration data, and registry manifests derive from the immutable material
catalogue. The unit suite parses every JSON, validates the OreSpawn provider
against the vendored 4.0.1 schema, follows model and texture references, checks
Base Metals registry references, and compares the current registry manifest to
the live 1.12 runtime baseline plus explicit restored and new IDs. The
pre-flattening numeric map is exported from the same captured `level.dat` by
`tools/validation/export_legacy_block_map.py`.

Tracked GameTests use `src/gametest/resources/gameteststructures/empty.snbt`.
The build copies this fixture into the isolated run directory before launching
Forge's GameTest server.

The validation-only `upgradeSmoke` source set is excluded from the release
JAR. `runServer -PupgradeWorldSmoke` checks the captured Forge 1.12 fixture,
including 20,553 state placements across all three vanilla dimensions, 1,038
exact inventory stacks, 96 equipped armor stacks, 33 filled-fluid buckets,
representative playerdata, and the stock and Base Metals OS3 rule files. Use
`-PupgradeWorldName=<copied-fixture-name>` to keep each direct-upgrade run
isolated. The production migrator runs Mojang's data-fixer path and overlays
only the third-party block states which Mojang cannot identify.

See [VALIDATION.md](VALIDATION.md) for the recorded release-gate, deterministic
world-generation, performance, JFR, and optional-integration results.
