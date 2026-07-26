package com.mcmoddev.basemetals.smoke;

import com.mcmoddev.basemetals.BaseMetals;
import com.mcmoddev.basemetals.ModTabs;
import com.mcmoddev.basemetals.client.MaterialProjectileRenderer;
import com.mcmoddev.basemetals.content.ModContent;
import com.mcmoddev.basemetals.entity.MaterialProjectile;
import com.mcmoddev.basemetals.entity.ModEntities;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.searchtree.SearchRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Development-only proof that the real client can create, join, tick, and
 * cleanly stop an integrated 1.18.2 server with Base Metals and OreSpawn.
 */
@Mod.EventBusSubscriber(modid = BaseMetals.MOD_ID, value = Dist.CLIENT)
public final class IntegratedWorldSmoke {
    private static final String ENABLED_PROPERTY = "basemetals.integratedWorldSmoke";
    private static final String WORLD_PROPERTY = "basemetals.integratedWorldSmokeName";
    private static final int READY_TICKS = 200;
    private static final int TIMEOUT_TICKS = 6000;

    private static int elapsedTicks;
    private static int joinedTicks;
    private static boolean creationStarted;
    private static boolean completed;
    private static boolean renderLayersChecked;
    private static boolean creativeBucketsChecked;
    private static boolean projectileRenderersChecked;
    private static boolean projectileSyncProbesSpawned;
    private static boolean projectileSyncChecked;

    private IntegratedWorldSmoke() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || completed
                || !Boolean.getBoolean(ENABLED_PROPERTY)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        elapsedTicks++;
        if (!renderLayersChecked) {
            verifyBlockRenderLayers();
            renderLayersChecked = true;
        }
        if (!creativeBucketsChecked) {
            verifyCreativeBuckets(minecraft);
            creativeBucketsChecked = true;
        }
        if (!creationStarted && minecraft.screen instanceof TitleScreen) {
            creationStarted = true;
            String worldName = System.getProperty(WORLD_PROPERTY, "basemetals-integrated-smoke");
            RegistryAccess.Frozen registries = RegistryAccess.BUILTIN.get();
            LevelSettings levelSettings = new LevelSettings(
                    "Base Metals Integrated Smoke",
                    GameType.CREATIVE,
                    false,
                    Difficulty.NORMAL,
                    true,
                    new GameRules(),
                    DataPackConfig.DEFAULT);
            BaseMetals.LOGGER.info("BASEMETALS_INTEGRATED_SMOKE creating world={}", worldName);
            minecraft.createLevel(worldName, levelSettings, registries, WorldGenSettings.makeDefault(registries));
        }

        if (minecraft.hasSingleplayerServer() && minecraft.getSingleplayerServer() != null
                && minecraft.level != null && minecraft.player != null) {
            ServerLevel serverLevel = minecraft.getSingleplayerServer().getLevel(minecraft.level.dimension());
            if (serverLevel != null && serverLevel.getServer().isRunning()) {
                if (!projectileRenderersChecked) {
                    verifyProjectileRenderers(minecraft);
                    projectileRenderersChecked = true;
                }
                if (!projectileSyncProbesSpawned && !serverLevel.players().isEmpty()) {
                    spawnProjectileSyncProbes(serverLevel);
                    projectileSyncProbesSpawned = true;
                }
                joinedTicks++;
                if (projectileSyncProbesSpawned && !projectileSyncChecked && joinedTicks >= 20) {
                    projectileSyncChecked = verifyProjectileSync(minecraft);
                    if (!projectileSyncChecked && joinedTicks >= 100) {
                        throw new IllegalStateException(
                                "BASEMETALS_INTEGRATED_SMOKE FAIL: projectile ammunition did not reach the client");
                    }
                }
                if (joinedTicks >= READY_TICKS) {
                    if (!projectileSyncChecked) {
                        throw new IllegalStateException(
                                "BASEMETALS_INTEGRATED_SMOKE FAIL: projectile sync was never verified");
                    }
                    completed = true;
                    BaseMetals.LOGGER.info(
                            "BASEMETALS_INTEGRATED_SMOKE PASS world={} dimension={} client_ticks={} server_ticks={}",
                            System.getProperty(WORLD_PROPERTY, "basemetals-integrated-smoke"),
                            minecraft.level.dimension().location(), elapsedTicks,
                            minecraft.getSingleplayerServer().getTickCount());
                    minecraft.stop();
                    return;
                }
            }
        }

        if (elapsedTicks >= TIMEOUT_TICKS) {
            completed = true;
            throw new IllegalStateException("BASEMETALS_INTEGRATED_SMOKE FAIL: timed out before a stable integrated world");
        }
    }

    private static void verifyBlockRenderLayers() {
        int bars = 0;
        int doors = 0;
        int trapdoors = 0;
        for (var reference : ModContent.blocksById().values()) {
            Block block = reference.get();
            RenderType expected = null;
            if (block instanceof IronBarsBlock) {
                expected = RenderType.cutoutMipped();
                bars++;
            } else if (block instanceof DoorBlock) {
                expected = RenderType.cutout();
                doors++;
            } else if (block instanceof TrapDoorBlock) {
                expected = RenderType.cutout();
                trapdoors++;
            }
            if (expected != null
                    && (!ItemBlockRenderTypes.canRenderInLayer(block.defaultBlockState(), expected)
                    || ItemBlockRenderTypes.canRenderInLayer(block.defaultBlockState(), RenderType.solid()))) {
                throw new IllegalStateException("BASEMETALS_INTEGRATED_SMOKE FAIL: wrong render layer for "
                        + block.getRegistryName());
            }
        }
        if (bars != 26 || doors != 26 || trapdoors != 26) {
            throw new IllegalStateException(
                    "BASEMETALS_INTEGRATED_SMOKE FAIL: incomplete cutout family bars=" + bars
                    + " doors=" + doors + " trapdoors=" + trapdoors);
        }
        BaseMetals.LOGGER.info(
                "BASEMETALS_INTEGRATED_SMOKE render layers PASS bars={} doors={} trapdoors={}",
                bars, doors, trapdoors);
    }

    private static void verifyCreativeBuckets(Minecraft minecraft) {
        NonNullList<ItemStack> itemsTab = NonNullList.create();
        NonNullList<ItemStack> searchTab = NonNullList.create();
        ModTabs.ITEMS.fillItemList(itemsTab);
        CreativeModeTab.TAB_SEARCH.fillItemList(searchTab);
        java.util.List<ItemStack> bucketSearch = minecraft
                .getSearchTree(SearchRegistry.CREATIVE_NAMES).search("bucket");
        for (var fluid : ModContent.fluids().entrySet()) {
            net.minecraft.world.item.Item bucket = fluid.getValue().bucket().get();
            ItemStack bucketStack = bucket.getDefaultInstance();
            if (itemsTab.stream().noneMatch(stack -> stack.is(bucket))
                    || searchTab.stream().noneMatch(stack -> stack.is(bucket))
                    || bucketSearch.stream().noneMatch(stack -> stack.is(bucket))) {
                throw new IllegalStateException(
                        "BASEMETALS_INTEGRATED_SMOKE FAIL: hidden creative bucket "
                                + fluid.getKey() + " direct_items=" + itemsTab.size()
                                + " direct_search=" + searchTab.size()
                                + " indexed_bucket_results=" + bucketSearch.size());
            }
            var model = minecraft.getItemRenderer().getModel(bucketStack, null, null, 0);
            if (model.getQuads(null, null, new java.util.Random(0L)).isEmpty()) {
                throw new IllegalStateException(
                        "BASEMETALS_INTEGRATED_SMOKE FAIL: invisible bucket model " + fluid.getKey());
            }
        }
        BaseMetals.LOGGER.info(
                "BASEMETALS_INTEGRATED_SMOKE creative buckets PASS count={}",
                ModContent.fluids().size());
    }

    private static void verifyProjectileRenderers(Minecraft minecraft) {
        for (var type : java.util.List.of(ModEntities.CUSTOM_ARROW.get(), ModEntities.CUSTOM_BOLT.get())) {
            MaterialProjectile projectile = new MaterialProjectile(type, minecraft.level);
            if (!(minecraft.getEntityRenderDispatcher().getRenderer(projectile)
                    instanceof MaterialProjectileRenderer)) {
                throw new IllegalStateException(
                        "BASEMETALS_INTEGRATED_SMOKE FAIL: projectile still uses a billboard renderer "
                                + type.getRegistryName());
            }
        }
        BaseMetals.LOGGER.info("BASEMETALS_INTEGRATED_SMOKE projectile renderers PASS");
    }

    private static void spawnProjectileSyncProbes(ServerLevel level) {
        var player = level.players().get(0);
        level.getServer().execute(() -> {
            MaterialProjectile arrow = new MaterialProjectile(ModEntities.CUSTOM_ARROW.get(), level, player,
                    ModContent.item("starsteel_arrow").get().getDefaultInstance());
            arrow.setNoGravity(true);
            arrow.setDeltaMovement(0.0D, 0.0D, 0.0D);
            level.addFreshEntity(arrow);

            MaterialProjectile bolt = new MaterialProjectile(ModEntities.CUSTOM_BOLT.get(), level, player,
                    ModContent.item("tin_bolt").get().getDefaultInstance());
            bolt.setNoGravity(true);
            bolt.setDeltaMovement(0.0D, 0.0D, 0.0D);
            level.addFreshEntity(bolt);
        });
    }

    private static boolean verifyProjectileSync(Minecraft minecraft) {
        var projectiles = minecraft.level.getEntitiesOfClass(MaterialProjectile.class,
                minecraft.player.getBoundingBox().inflate(16.0D));
        boolean arrow = projectiles.stream().anyMatch(projectile ->
                projectile.getType() == ModEntities.CUSTOM_ARROW.get()
                        && projectile.getItem().is(ModContent.item("starsteel_arrow").get()));
        boolean bolt = projectiles.stream().anyMatch(projectile ->
                projectile.getType() == ModEntities.CUSTOM_BOLT.get()
                        && projectile.getItem().is(ModContent.item("tin_bolt").get()));
        if (arrow && bolt) {
            BaseMetals.LOGGER.info(
                    "BASEMETALS_INTEGRATED_SMOKE projectile ammunition sync PASS");
            return true;
        }
        return false;
    }
}
