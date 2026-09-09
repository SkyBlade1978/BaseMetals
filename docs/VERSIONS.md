# Supported versions

Base Metals `3.0.1.113021` targets Minecraft `1.13.2`, Forge `25.0.223`, and
Java 8. The Gradle build itself runs on Java `17.0.1+12` and uses ForgeGradle
`7.0.34` with Gradle `9.6.1`; those build-time runtimes do not change the Java
8 requirement of the released mod.

Development, CI, and release qualification use the exact public OreSpawn
`4.0.16.113021` release (CurseForge project `245586`, file `8836169`, SHA-256
`38C091390486AFCEF8F43404071AF595E53FF5750DB99E0F8F851858C85BA11C`).
The runtime contract is OreSpawn `[4.0.16.113021,5.0.0)` for Minecraft 1.13.2.

Release artifacts use Maven coordinate
`zone.moddev.mc.basemetals:BaseMetals:3.0.1.113021`.
