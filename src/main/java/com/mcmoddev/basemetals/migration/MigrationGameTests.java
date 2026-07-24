package com.mcmoddev.basemetals.migration;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.mcmoddev.basemetals.BaseMetals;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Direct coverage of the pre-flattening overlay around Mojang's data fixer. */
@GameTestHolder(BaseMetals.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MigrationGameTests {
    private MigrationGameTests() {}

    @GameTest(template = "empty")
    public static void vanillaFixesAndMmdlibStatesSurviveTheSameChunkConversion(GameTestHelper helper) {
        try {
            CompoundTag converted = Legacy112WorldMigrator.convertChunkForTest(legacyChunk(), legacyIds());
            require(helper, converted.getInt("DataVersion") == 1_456,
                    "Migration did not stop at the safe post-flattening data version");

            CompoundTag vanillaLower = Legacy112WorldMigrator.stateAtForTest(converted, 5, index(0, 1, 0));
            CompoundTag vanillaUpper = Legacy112WorldMigrator.stateAtForTest(converted, 5, index(0, 2, 0));
            require(helper, vanillaLower.getString("Name").equals("minecraft:oak_door"),
                    "Mojang's vanilla wooden-door flattening did not run");
            require(helper, hasDoorProperties(vanillaLower) && hasDoorProperties(vanillaUpper),
                    "Mojang's context-sensitive vanilla door fix did not merge both halves");

            CompoundTag aliasLower = Legacy112WorldMigrator.stateAtForTest(converted, 5, index(2, 1, 0));
            CompoundTag aliasUpper = Legacy112WorldMigrator.stateAtForTest(converted, 5, index(2, 2, 0));
            require(helper, aliasLower.getString("Name").equals("basemetals:quartz_door")
                    && aliasUpper.getString("Name").equals("basemetals:quartz_door"),
                    "mmdlib block aliases were not restored under Base Metals");
            require(helper, hasDoorProperties(aliasLower) && hasDoorProperties(aliasUpper),
                    "Base Metals door halves did not receive a complete modern state");
            require(helper, aliasLower.getCompound("Properties").getString("open").equals("true")
                    && aliasUpper.getCompound("Properties").getString("hinge").equals("right")
                    && aliasUpper.getCompound("Properties").getString("powered").equals("true"),
                    "Base Metals door metadata was not merged across its two halves");

            CompoundTag plate = Legacy112WorldMigrator.stateAtForTest(converted, 5, index(4, 1, 0));
            require(helper, plate.getString("Name").equals("basemetals:copper_plate")
                    && plate.getCompound("Properties").getString("facing").equals("east"),
                    "mmdlib metadata was lost while forwarding a stateful block");

            CompoundTag chest = converted.getCompound("Level").getList("TileEntities", Tag.TAG_COMPOUND)
                    .getCompound(0);
            CompoundTag bucket = chest.getList("Items", Tag.TAG_COMPOUND).getCompound(0);
            require(helper, bucket.getString("id").equals("basemetals:mercury_bucket"),
                    "Legacy Forge universal bucket did not become its dedicated Base Metals bucket");
            require(helper, !bucket.contains("tag", Tag.TAG_COMPOUND)
                    || !bucket.getCompound("tag").contains("FluidName", Tag.TAG_STRING),
                    "Obsolete universal-bucket fluid metadata survived conversion");
            CompoundTag damagedTool = chest.getList("Items", Tag.TAG_COMPOUND).getCompound(1);
            require(helper, damagedTool.getCompound("tag").getInt("Damage") == 12,
                    "Legacy mod-item damage was consumed as flattening metadata");
        } catch (IOException | RuntimeException exception) {
            helper.fail("Legacy chunk conversion failed: " + exception);
            return;
        }
        helper.succeed();
    }

    private static CompoundTag legacyChunk() {
        byte[] blocks = new byte[4_096];
        byte[] metadata = new byte[2_048];
        byte[] add = new byte[2_048];
        set(blocks, metadata, add, index(0, 1, 0), 64, 4);
        set(blocks, metadata, add, index(0, 2, 0), 64, 11);
        set(blocks, metadata, add, index(2, 1, 0), 300, 4);
        set(blocks, metadata, add, index(2, 2, 0), 300, 11);
        set(blocks, metadata, add, index(4, 1, 0), 301, 5);

        CompoundTag section = new CompoundTag();
        section.putByte("Y", (byte) 5);
        section.put("Blocks", new ByteArrayTag(blocks));
        section.put("Data", new ByteArrayTag(metadata));
        section.put("Add", new ByteArrayTag(add));
        section.put("BlockLight", new ByteArrayTag(new byte[2_048]));
        section.put("SkyLight", new ByteArrayTag(new byte[2_048]));
        ListTag sections = new ListTag();
        sections.add(section);

        CompoundTag level = new CompoundTag();
        level.putInt("xPos", 0);
        level.putInt("zPos", 0);
        level.putLong("LastUpdate", 0L);
        level.putLong("InhabitedTime", 0L);
        level.putBoolean("TerrainPopulated", true);
        level.putBoolean("LightPopulated", true);
        level.put("HeightMap", new IntArrayTag(new int[256]));
        level.put("Biomes", new ByteArrayTag(new byte[256]));
        level.put("Sections", sections);
        level.put("Entities", new ListTag());
        CompoundTag bucketTag = new CompoundTag();
        bucketTag.putString("FluidName", "liquid_mercury");
        bucketTag.putInt("Amount", 1_000);
        CompoundTag bucket = new CompoundTag();
        bucket.putString("id", "forge:bucketfilled");
        bucket.putByte("Count", (byte) 1);
        bucket.putShort("Damage", (short) 0);
        bucket.putByte("Slot", (byte) 0);
        bucket.put("tag", bucketTag);
        ListTag items = new ListTag();
        items.add(bucket);
        CompoundTag damagedTool = new CompoundTag();
        damagedTool.putString("id", "basemetals:copper_pickaxe");
        damagedTool.putByte("Count", (byte) 1);
        damagedTool.putShort("Damage", (short) 12);
        damagedTool.putByte("Slot", (byte) 1);
        items.add(damagedTool);
        CompoundTag chest = new CompoundTag();
        chest.putString("id", "minecraft:chest");
        chest.putInt("x", 8);
        chest.putInt("y", 80);
        chest.putInt("z", 8);
        chest.put("Items", items);
        ListTag tileEntities = new ListTag();
        tileEntities.add(chest);
        level.put("TileEntities", tileEntities);
        level.put("TileTicks", new ListTag());

        CompoundTag chunk = new CompoundTag();
        chunk.putInt("DataVersion", 1_343);
        chunk.put("Level", level);
        return chunk;
    }

    private static Map<Integer, String> legacyIds() {
        Map<Integer, String> ids = new LinkedHashMap<>();
        ids.put(0, "minecraft:air");
        ids.put(64, "minecraft:wooden_door");
        ids.put(300, "mmdlib:quartz_door");
        ids.put(301, "mmdlib:copper_plate");
        return ids;
    }

    private static int index(int x, int y, int z) {
        return y << 8 | z << 4 | x;
    }

    private static void set(byte[] blocks, byte[] metadata, byte[] add, int index, int id, int meta) {
        blocks[index] = (byte) id;
        setNibble(add, index, id >>> 8);
        setNibble(metadata, index, meta);
    }

    private static void setNibble(byte[] values, int index, int value) {
        int slot = index >>> 1;
        int shift = (index & 1) * 4;
        values[slot] = (byte) (values[slot] & ~(15 << shift) | (value & 15) << shift);
    }

    private static boolean hasDoorProperties(CompoundTag state) {
        if (!state.contains("Properties", Tag.TAG_COMPOUND)) return false;
        CompoundTag properties = state.getCompound("Properties");
        return properties.contains("half", Tag.TAG_STRING)
                && properties.contains("facing", Tag.TAG_STRING)
                && properties.contains("open", Tag.TAG_STRING)
                && properties.contains("hinge", Tag.TAG_STRING)
                && properties.contains("powered", Tag.TAG_STRING);
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
