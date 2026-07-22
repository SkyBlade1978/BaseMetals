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
including all 6,851 state placements and all 1,038 inventory items, after the
production pre-flattening and vanilla data-fixer path.

See [VALIDATION.md](VALIDATION.md) for the recorded release-gate, deterministic
world-generation, performance, JFR, and optional-integration results.
