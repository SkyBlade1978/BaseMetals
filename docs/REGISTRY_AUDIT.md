# 1.12 registry audit

The compatibility baseline is a live Forge 1.12.2 capture, not a source-code
estimate. It used Forge 14.23.5.2847, Java 8, and these public artifacts:

- `BaseMetals-1.12-2.5.0-rc2.332.jar`, SHA-256
  `034FD791A9E77345C19F6098C01D4B5B66F90EF4A953FB0831089935EAD9DFFB`;
- `MMDLib-1.12-1.0.0-rc2.36.jar`, SHA-256
  `FE2229E6755A5FD306C33CC22DCCF055378D371339133E4912C5DC9213E360AF`.

The capture found 354 registered Base Metals blocks, 1,038 items, 33 source
fluids, and 6,851 valid block states. Fixture format 2 places all 6,851 states
in each of the Overworld, Nether, and End, stores every item in an exact
non-adjacent chest slot with representative damage, enchantment, and custom
NBT, equips all armor on stands, stores every legacy filled-fluid bucket, and
saves a representative player inventory. The compact machine-readable
registry baseline is tracked at
`reference/1.12/runtime/registry_manifest.json` and is copied into the
generated `data/basemetals/registry_manifest_1_12.json` resource.

The 1.18 catalogue deliberately restores six blocks, 41 corresponding or
historically intended items, and three source fluids which were present in the
retained feature source but absent from the last public release runtime. It
adds 36 dedicated buckets, 36 flowing fluids, two projectile entities, the
crushing serializer, and the global-loot-modifier serializer. Automated tests
require the current registry to equal the live runtime baseline plus only
those named allowlists.

Forge 1.12 stored modded block states as numeric IDs. The release therefore
also packages the exact 608-entry numeric block map captured from the fixture's
`level.dat`. The export scripts under `tools/validation` reproduce both
tracked reference files.
