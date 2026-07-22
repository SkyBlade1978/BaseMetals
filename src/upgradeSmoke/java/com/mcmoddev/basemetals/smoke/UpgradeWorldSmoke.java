package com.mcmoddev.basemetals.smoke;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcmoddev.basemetals.BaseMetals;
import com.mcmoddev.basemetals.migration.Legacy112WorldMigrator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Validation-only reader for the captured Forge 1.12 upgrade fixture. */
@Mod.EventBusSubscriber(modid = BaseMetals.MOD_ID)
public final class UpgradeWorldSmoke {
    private static final String ENABLED_PROPERTY = "basemetals.upgradeWorldSmoke";

    private UpgradeWorldSmoke() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!Boolean.getBoolean(ENABLED_PROPERTY)) return;

        Path root = event.getServer().getWorldPath(LevelResource.ROOT);
        Path manifestPath = root.resolve("legacy_registry_manifest_runtime.json");
        Path marker = root.resolve("BASEMETALS_1_12_FIXTURE_COMPLETE.txt");
        List<String> failures = new ArrayList<>();
        int blockMismatches = 0;
        int itemMismatches = 0;
        int checkedStates = 0;
        int checkedItems = 0;
        try {
            if (!Files.isRegularFile(marker)) failures.add("missing legacy fixture marker");
            JsonObject manifest;
            try (Reader reader = Files.newBufferedReader(manifestPath, StandardCharsets.UTF_8)) {
                manifest = JsonParser.parseReader(reader).getAsJsonObject();
            }

            ServerLevel world = event.getServer().overworld();
            for (JsonElement blockElement : manifest.getAsJsonArray("blocks")) {
                JsonObject block = blockElement.getAsJsonObject();
                String expected = block.get("id").getAsString();
                for (JsonElement stateElement : block.getAsJsonArray("states")) {
                    JsonObject oldState = stateElement.getAsJsonObject();
                    BlockPos pos = new BlockPos(oldState.get("x").getAsInt(), oldState.get("y").getAsInt(),
                            oldState.get("z").getAsInt());
                    String actual = String.valueOf(ForgeRegistries.BLOCKS.getKey(world.getBlockState(pos).getBlock()));
                    boolean stateMatches = Legacy112WorldMigrator.matchesLegacyMetadata(world.getBlockState(pos),
                            oldState.get("metadata").getAsInt());
                    if (!expected.equals(actual) || !stateMatches) {
                        if (blockMismatches < 10) {
                            failures.add("block " + pos + " expected " + expected + " metadata="
                                    + oldState.get("metadata").getAsInt() + " but found "
                                    + world.getBlockState(pos));
                        }
                        blockMismatches++;
                    }
                    checkedStates++;
                }
            }

            Map<String, Integer> expectedItems = new LinkedHashMap<>();
            int highestChest = -1;
            for (JsonElement itemElement : manifest.getAsJsonArray("items")) {
                JsonObject oldItem = itemElement.getAsJsonObject();
                String expected = oldItem.get("id").getAsString();
                int chestIndex = oldItem.get("chest").getAsInt();
                expectedItems.merge(expected, 1, Integer::sum);
                highestChest = Math.max(highestChest, chestIndex);
                checkedItems++;
            }

            Map<String, Integer> actualItems = new LinkedHashMap<>();
            for (int chestIndex = 0; chestIndex <= highestChest; chestIndex++) {
                BlockPos pos = gridPosition(chestIndex, -64, 120);
                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (!(blockEntity instanceof ChestBlockEntity chest)) {
                    if (itemMismatches < 10) failures.add("missing inventory chest " + chestIndex + " at " + pos);
                    itemMismatches++;
                } else {
                    for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                        ItemStack stack = chest.getItem(slot);
                        if (!stack.isEmpty()) {
                            String actual = String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()));
                            actualItems.merge(actual, stack.getCount(), Integer::sum);
                        }
                    }
                }
            }
            for (String id : union(expectedItems, actualItems)) {
                int expected = expectedItems.getOrDefault(id, 0);
                int actual = actualItems.getOrDefault(id, 0);
                if (expected != actual) {
                    if (itemMismatches < 10) {
                        failures.add("inventory expected " + expected + " of " + id + " but found " + actual);
                    }
                    itemMismatches += Math.abs(expected - actual);
                }
            }
        } catch (IOException | RuntimeException exception) {
            failures.add(exception.toString());
        }

        if (!failures.isEmpty()) {
            String result = "BASEMETALS_UPGRADE_SMOKE FAIL states=" + checkedStates
                    + " block_mismatches=" + blockMismatches + " items=" + checkedItems
                    + " item_mismatches=" + itemMismatches + " samples=" + failures;
            writeResult(root, result);
            throw new IllegalStateException(result);
        }

        writeResult(root, "BASEMETALS_UPGRADE_SMOKE PASS states=" + checkedStates + " items=" + checkedItems);
        BaseMetals.LOGGER.info("BASEMETALS_UPGRADE_SMOKE PASS states={} items={} world={}",
                checkedStates, checkedItems, root);
        event.getServer().halt(false);
    }

    private static BlockPos gridPosition(int ordinal, int xOffset, int y) {
        return new BlockPos(xOffset + ordinal % 64, y, ordinal / 64);
    }

    private static Iterable<String> union(Map<String, Integer> first, Map<String, Integer> second) {
        Map<String, Integer> union = new LinkedHashMap<>(first);
        second.forEach(union::putIfAbsent);
        return union.keySet();
    }

    private static void writeResult(Path root, String result) {
        try {
            Files.writeString(root.resolve("BASEMETALS_UPGRADE_SMOKE_RESULT.txt"), result + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            BaseMetals.LOGGER.error("Could not write upgrade smoke result", exception);
        }
    }
}
