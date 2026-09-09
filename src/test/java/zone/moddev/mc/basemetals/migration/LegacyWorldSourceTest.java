package zone.moddev.mc.basemetals.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

class LegacyWorldSourceTest {
    @TempDir
    Path temporaryWorld;

    @Test
    void usesTheUntouchedLegacyRegistryAfterForgeRewritesLevelDat() throws IOException {
        writeLevel("level.dat", 2_975, 1, "minecraft:stone");
        writeLevel("level.dat_old", 512, 1_976, "basemetals:copper_ore");

        Map<Integer, String> ids = LegacyWorldDataHook.legacyBlockIdsForTest(temporaryWorld);

        assertEquals("basemetals:copper_ore", ids.get(1_976));
    }

    @Test
    void normalizesRegistryPathsThatWereLegalInMinecraft110() {
        assertEquals("mca:rosegoldore",
                LegacyWorldDataHook.normalizeLegacyRegistryName("mca:RoseGoldOre"));
        assertEquals("basemetals:copper_ore",
                LegacyWorldDataHook.normalizeLegacyRegistryName("basemetals:copper_ore"));
    }

    @Test
    void repairsTheBrokenForge25LeavesFixGenericCallBeforeLegacyChunksLoad() throws IOException {
        String coremod = new String(Files.readAllBytes(Paths.get(
                "src/main/resources/coremods/basemetals_113_compatibility.js")), "UTF-8");
        assertTrue(coremod.contains("basemetals_forge25_leaves_fixer"));
        assertTrue(coremod.contains("Ljava/lang/Object;)Lcom/mojang/datafixers/Typed;"));
    }

    private void writeLevel(String name, int dataVersion, int blockId, String blockName) throws IOException {
        NBTTagCompound entry = new NBTTagCompound();
        entry.setInt("V", blockId);
        entry.setString("K", blockName);
        NBTTagList ids = new NBTTagList();
        ids.add(entry);

        NBTTagCompound blocks = new NBTTagCompound();
        blocks.setTag("ids", ids);
        NBTTagCompound registries = new NBTTagCompound();
        registries.setTag("minecraft:blocks", blocks);
        NBTTagCompound fml = new NBTTagCompound();
        fml.setTag("Registries", registries);
        NBTTagCompound data = new NBTTagCompound();
        data.setInt("DataVersion", dataVersion);
        NBTTagCompound level = new NBTTagCompound();
        level.setTag("FML", fml);
        level.setTag("Data", data);

        try (FileOutputStream output = new FileOutputStream(temporaryWorld.resolve(name).toFile())) {
            CompressedStreamTools.writeCompressed(level, output);
        }
    }
}
