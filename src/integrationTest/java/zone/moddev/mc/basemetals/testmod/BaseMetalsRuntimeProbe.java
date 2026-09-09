package zone.moddev.mc.basemetals.testmod;

import java.util.UUID;
import java.util.Map;

import com.mojang.authlib.GameProfile;

import zone.moddev.mc.basemetals.BaseMetals;
import zone.moddev.mc.basemetals.ModTabs;
import zone.moddev.mc.basemetals.content.BaseMetalAnvilBlock;
import zone.moddev.mc.basemetals.content.BaseMetalAmmoItem;
import zone.moddev.mc.basemetals.content.FluidContent;
import zone.moddev.mc.basemetals.content.MaterialItems;
import zone.moddev.mc.basemetals.content.ModContent;
import zone.moddev.mc.basemetals.content.PlateBlock;
import zone.moddev.mc.basemetals.entity.MaterialProjectile;
import zone.moddev.mc.basemetals.entity.ModEntities;
import zone.moddev.mc.basemetals.material.MaterialCatalogue;
import zone.moddev.mc.basemetals.migration.LegacyWorldDataHook;
import zone.moddev.mc.basemetals.recipe.CrushingRecipe;

import net.minecraft.block.BlockAnvil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.entity.Entity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.registries.ForgeRegistries;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Isolated runtime assertions; this class is never part of the public JAR. */
@Mod(BaseMetalsRuntimeProbe.MODID)
public final class BaseMetalsRuntimeProbe {
    static final String MODID = "basemetalsprobe";
    private static final Logger LOGGER = LogManager.getLogger(MODID);
    private int checks;

    public BaseMetalsRuntimeProbe() {
        MinecraftForge.EVENT_BUS.addListener(this::serverStarted);
    }

    private void serverStarted(FMLServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        boolean legacyUpgrade = "legacy-upgrade".equals(System.getProperty("basemetalsprobe.mode"));
        try {
            runChecks(server);
            if (legacyUpgrade) verifyLegacyBlocks(server);
            if (checks < 32) throw new IllegalStateException("Only ran " + checks + " runtime checks");
            LOGGER.info(legacyUpgrade ? "BASEMETALS_LEGACY_UPGRADE_PROBE PASS checks={}"
                    : "BASEMETALS_RUNTIME_PROBE PASS checks={}", Integer.valueOf(checks));
        } catch (Throwable failure) {
            LOGGER.error(legacyUpgrade ? "BASEMETALS_LEGACY_UPGRADE_PROBE FAIL after {} checks"
                    : "BASEMETALS_RUNTIME_PROBE FAIL after {} checks", Integer.valueOf(checks), failure);
            throw failure instanceof RuntimeException ? (RuntimeException) failure
                    : new IllegalStateException(failure);
        } finally {
            server.initiateShutdown();
        }
    }

    private void verifyLegacyBlocks(MinecraftServer server) {
        WorldServer world = require(server.getWorld(DimensionType.OVERWORLD), "legacy overworld missing");
        Map<Long, Integer> expected = LegacyWorldDataHook.legacyPreparedBlockCounts();
        int expectedBlocks = 0;
        int convertedBlocks = 0;
        int verifiedChunks = 0;
        for (Chunk chunk : world.getChunkProvider().getLoadedChunks()) {
            Integer expectedInChunk = expected.get(Long.valueOf(chunkKey(chunk.x, chunk.z)));
            if (expectedInChunk == null) continue;
            int actualInChunk = 0;
            for (ChunkSection section : chunk.getSections()) {
                if (section == null || section.isEmpty()) continue;
                for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
                    ResourceLocation id = ForgeRegistries.BLOCKS.getKey(section.get(x, y, z).getBlock());
                    if (id != null && BaseMetals.MOD_ID.equals(id.getNamespace())) actualInChunk++;
                }
            }
            check(actualInChunk == expectedInChunk.intValue(),
                    "legacy block count in chunk " + chunk.x + "," + chunk.z);
            expectedBlocks += expectedInChunk.intValue();
            convertedBlocks += actualInChunk;
            verifiedChunks++;
        }
        check(verifiedChunks > 0, "legacy chunks with Base Metals content loaded");
        check(expectedBlocks > 0 && convertedBlocks == expectedBlocks,
                "legacy Base Metals blocks survived flattening");
        LOGGER.info("BASEMETALS_LEGACY_BLOCKS VERIFIED blocks={} chunks={}",
                Integer.valueOf(convertedBlocks), Integer.valueOf(verifiedChunks));
    }

    private void runChecks(MinecraftServer server) throws Exception {
        WorldServer world = require(server.getWorld(DimensionType.OVERWORLD), "overworld missing");
        check(MaterialCatalogue.ALL.size() == 22, "material catalogue size");
        check(ModContent.blocksById().size() == 360, "registered block catalogue size");
        check(ModContent.itemsById().size() == 1115, "registered item catalogue size");
        check(ModContent.fluids().size() == 36, "fluid family count");
        check(ForgeRegistries.ENTITIES.getKey(ModEntities.CUSTOM_ARROW.get()).equals(id("custom_arrow")),
                "custom arrow entity id");
        check(ForgeRegistries.ENTITIES.getKey(ModEntities.CUSTOM_BOLT.get()).equals(id("custom_bolt")),
                "custom bolt entity id");

        for (java.util.Map.Entry<String, zone.moddev.mc.basemetals.content.RegistryHandle<net.minecraft.block.Block>>
                entry : ModContent.blocksById().entrySet()) {
            check(ForgeRegistries.BLOCKS.getKey(entry.getValue().get()).equals(id(entry.getKey())),
                    "block id " + entry.getKey());
        }
        for (java.util.Map.Entry<String, zone.moddev.mc.basemetals.content.RegistryHandle<Item>>
                entry : ModContent.itemsById().entrySet()) {
            check(ForgeRegistries.ITEMS.getKey(entry.getValue().get()).equals(id(entry.getKey())),
                    "item id " + entry.getKey());
        }

        check(ModContent.hiddenBlocks().size() == 26, "hidden double slab count");
        for (String name : ModContent.hiddenBlocks()) {
            check(ModContent.blocksById().get(name).get().getDefaultState()
                    .get(BlockSlab.TYPE).getName().equals("double"),
                    "hidden double slab state " + name);
            check(!ForgeRegistries.ITEMS.containsKey(id(name)), "hidden block item " + name);
        }

        for (java.util.Map.Entry<String, FluidContent> entry : ModContent.fluids().entrySet()) {
            String name = entry.getKey();
            FluidContent fluid = entry.getValue();
            check(IRegistry.field_212619_h.getKey(fluid.source().get()).equals(id(name)),
                    "source fluid " + name);
            check(IRegistry.field_212619_h.getKey(fluid.flowing().get()).equals(id("flowing_" + name)),
                    "flowing fluid " + name);
            check(ForgeRegistries.BLOCKS.getKey(fluid.block().get()).equals(id(name)), "fluid block " + name);
            check(ForgeRegistries.ITEMS.getKey(fluid.bucket().get()).equals(id(name + "_bucket")),
                    "fluid bucket " + name);
            check(fluid.bucket().get().getGroup() == ModTabs.ITEMS, "bucket creative tab " + name);
        }

        checkRecipe(server, "iron_ore_crushing", "iron_powder", 2);
        checkRecipe(server, "coal_ore_crushing", "coal_powder", 2);
        checkRecipe(server, "gravel_crushing", "minecraft:sand", 1);
        checkRecipe(server, "iron_powder_smelting", "minecraft:iron_ingot", 1);
        checkRecipe(server, "steel_blend_smelting", "steel_ingot", 1);
        check(server.getRecipeManager().getRecipes().size() >= 2000, "recipe catalogue loaded");

        Block plate = ModContent.blocksById().get("copper_plate").get();
        IBlockState up = plate.getDefaultState().with(PlateBlock.FACING, EnumFacing.UP);
        BlockPos origin = new BlockPos(0, 0, 0);
        AxisAlignedBB plateBounds = plate.getShape(up, world, origin).getBoundingBox();
        check(plateBounds.equals(new AxisAlignedBB(0, 0, 0, 1, 1.0D / 16.0D, 1)),
                "plate placement bounds");

        Block detector = ModContent.HUMAN_DETECTOR.get();
        IBlockState detectorState = detector.getDefaultState();
        check(detector.getWeakPower(detectorState, world, origin, EnumFacing.UP) == 0,
                "detector has no signal without players");

        BlockAnvil anvil = (BlockAnvil) ModContent.blocksById().get("steel_anvil").get();
        IBlockState intact = anvil.getDefaultState();
        IBlockState chipped = BlockAnvil.damage(intact);
        check(chipped != null && chipped.get(BaseMetalAnvilBlock.DAMAGE).intValue() == 1,
                "custom anvil first damage");
        IBlockState damaged = BlockAnvil.damage(chipped);
        check(damaged != null && damaged.get(BaseMetalAnvilBlock.DAMAGE).intValue() == 2,
                "custom anvil second damage");
        check(BlockAnvil.damage(damaged) == null, "custom anvil final damage");

        Object providerStatus = Class.forName("zone.moddev.mc.orespawn.api.OreSpawnApi")
                .getMethod("getProviderStatus", String.class).invoke(null, BaseMetals.MOD_ID);
        check("ACTIVE".equals(String.valueOf(providerStatus)), "OreSpawn provider active");
        check(classMissing("zone.moddev.mc.basemetals.worldgen.BaseMetalsOreGenerator"),
                "native world generator absent");

        check(new ItemTags.Wrapper(new ResourceLocation("forge", "ingots/copper"))
                .contains(ModContent.item("copper_ingot").get()), "copper ingot tag");
        check(new ItemTags.Wrapper(new ResourceLocation("forge", "ingots/adamant"))
                .contains(ModContent.item("adamantine_ingot").get()), "adamant alias tag");

        LootTable chest = server.getLootTableManager().getLootTableFromLocation(
                id("chests/inject/simple_dungeon"));
        check(chest != LootTable.EMPTY_LOOT_TABLE, "auxiliary chest loot table");
        check(server.getAdvancementManager().getAdvancement(id("steel_maker")) != null,
                "steel advancement loaded");
        check(server.getAdvancementManager().getAllAdvancements().stream()
                .filter(value -> BaseMetals.MOD_ID.equals(value.getId().getNamespace())).count() == 18,
                "eighteen advancements loaded");

        testProjectilePersistence(world);
        testStarsteelRepair(world);
        testAdamantineArmor(world);
        testShieldUpgrade();
        testCrossbowContract();
    }

    private void testProjectilePersistence(WorldServer world) {
        ItemStack ammunition = new ItemStack(ModContent.item("copper_arrow").get(), 8);
        EntityPlayer player = new QuietFakePlayer(world,
                new GameProfile(UUID.fromString("00000000-0000-0000-0000-000000000113"), "ArmorProbe"));
        MaterialProjectile original = new MaterialProjectile(ModEntities.CUSTOM_ARROW.get(), world, player,
                ammunition);
        NBTTagCompound tag = new NBTTagCompound();
        ((Entity) original).writeWithoutTypeId(tag);
        MaterialProjectile restored = new MaterialProjectile(ModEntities.CUSTOM_ARROW.get(), world);
        ((Entity) restored).read(tag);
        check(restored.getAmmunition().getItem() == ModContent.item("copper_arrow").get()
                && restored.getAmmunition().getCount() == 1, "projectile ammunition persistence");
    }

    private void testStarsteelRepair(WorldServer world) {
        EntityPlayer player = FakePlayerFactory.getMinecraft(world);
        ItemStack tool = new ItemStack(ModContent.item("starsteel_pickaxe").get());
        tool.setDamage(5);
        player.setHeldItem(EnumHand.MAIN_HAND, tool);
        player.ticksExisted = 200;
        MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
        check(tool.getDamage() == 4, "starsteel held repair");
    }

    private void testAdamantineArmor(WorldServer world) {
        EntityPlayer player = new QuietFakePlayer(world,
                new GameProfile(UUID.fromString("00000000-0000-0000-0000-000000000114"), "ArmorProbe"));
        player.setItemStackToSlot(EntityEquipmentSlot.HEAD,
                new ItemStack(ModContent.item("adamantine_helmet").get()));
        player.setItemStackToSlot(EntityEquipmentSlot.CHEST,
                new ItemStack(ModContent.item("adamantine_chestplate").get()));
        player.setItemStackToSlot(EntityEquipmentSlot.LEGS,
                new ItemStack(ModContent.item("adamantine_leggings").get()));
        player.setItemStackToSlot(EntityEquipmentSlot.FEET,
                new ItemStack(ModContent.item("adamantine_boots").get()));
        player.ticksExisted = 20;
        MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
        check(player.isPotionActive(MobEffects.RESISTANCE)
                && player.getActivePotionEffect(MobEffects.RESISTANCE).getAmplifier() == 1,
                "adamantine full-set resistance II");
    }

    private void testShieldUpgrade() {
        ItemStack shield = new ItemStack(ModContent.item("copper_shield").get());
        ItemStack plate = new ItemStack(ModContent.item("steel_plate").get());
        AnvilUpdateEvent event = new AnvilUpdateEvent(shield, plate, "", 0);
        MinecraftForge.EVENT_BUS.post(event);
        check(event.getOutput().getItem() == ModContent.item("steel_shield").get()
                && event.getMaterialCost() == 1 && event.getCost() >= 5, "shield plate upgrade");
    }

    private void testCrossbowContract() throws Exception {
        Item crossbow = ModContent.item("steel_crossbow").get();
        check(crossbow instanceof MaterialItems.Crossbow, "crossbow implementation");
        check(((BaseMetalAmmoItem) ModContent.item("steel_bolt").get()).kind()
                == BaseMetalAmmoItem.Kind.BOLT, "bolt item kind");
    }

    private void checkRecipe(MinecraftServer server, String recipeName, String resultName, int count) {
        IRecipe recipe = require(server.getRecipeManager().getRecipe(id(recipeName)),
                "missing recipe " + recipeName);
        ResourceLocation expected = resultName.indexOf(':') >= 0
                ? new ResourceLocation(resultName) : id(resultName);
        check(ForgeRegistries.ITEMS.getKey(recipe.getRecipeOutput().getItem()).equals(expected)
                && recipe.getRecipeOutput().getCount() == count, "recipe " + recipeName);
        if (recipeName.endsWith("_crushing")) check(recipe instanceof CrushingRecipe,
                "crushing serializer " + recipeName);
    }

    private void check(boolean condition, String description) {
        if (!condition) throw new IllegalStateException("Runtime check failed: " + description);
        checks++;
    }

    private static boolean classMissing(String name) {
        try { Class.forName(name); return false; }
        catch (ClassNotFoundException expected) { return true; }
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(BaseMetals.MOD_ID, path);
    }

    private static long chunkKey(int x, int z) {
        return ((long) x & 0xffffffffL) << 32 | ((long) z & 0xffffffffL);
    }

    private static <T> T require(T value, String message) {
        if (value == null) throw new IllegalStateException(message);
        return value;
    }

    /** FakePlayer has no network connection, so potion callbacks must not send packets. */
    private static final class QuietFakePlayer extends FakePlayer {
        private QuietFakePlayer(WorldServer world, GameProfile profile) { super(world, profile); }
        @Override protected void onNewPotionEffect(PotionEffect effect) {}
        @Override protected void onChangedPotionEffect(PotionEffect effect, boolean reapply) {}
        @Override protected void onFinishedPotionEffect(PotionEffect effect) {}
    }
}
