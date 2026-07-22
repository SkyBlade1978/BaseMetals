# Forge 1.12 runtime reference

`registry_manifest.json` is the compact, tracked form of a live Forge 1.12.2
capture made with Base Metals 2.5.0-rc2.332 and MMDLib 1.0.0-rc2.36. The
capture enumerated every registered Base Metals block, valid block state,
item, and source fluid, placed the block states in a world, and stored every
item in chests before saving and reopening the fixture under Forge 1.12.2.

The full coordinate/state manifest and fixture world are validation outputs,
not release resources. Regenerate this compact reference with:

```powershell
.\tools\validation\compact_runtime_manifest.ps1 `
  -Capture <path-to-full-capture> `
  -Output .\reference\1.12\runtime\registry_manifest.json
```

The artifact hashes in the manifest identify the exact public runtime inputs.
