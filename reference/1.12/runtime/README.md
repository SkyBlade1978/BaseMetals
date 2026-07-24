# Forge 1.12 runtime reference

`registry_manifest.json` is the compact, tracked form of a live Forge 1.12.2
capture made with Base Metals 2.5.0-rc2.332 and MMDLib 1.0.0-rc2.36. The
capture enumerated every registered Base Metals block, valid block state,
item, and source fluid. Fixture format 2 places all 6,851 states independently
in each vanilla dimension, stores all 1,038 item stacks in exact chest slots,
equips representative damaged armor, stores every legacy filled-fluid bucket,
and writes representative playerdata before saving and reopening the world
under Forge 1.12.2.

The full coordinate/state manifest, OS3 configuration samples, and fixture
world are validation outputs, not release resources. Regenerate this compact
registry reference with:

```powershell
.\tools\validation\compact_runtime_manifest.ps1 `
  -Capture <path-to-full-capture> `
  -Output .\reference\1.12\runtime\registry_manifest.json
```

The artifact hashes in the manifest identify the exact public runtime inputs.
