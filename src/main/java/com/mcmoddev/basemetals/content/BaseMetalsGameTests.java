package com.mcmoddev.basemetals.content;

import com.mcmoddev.basemetals.BaseMetals;
import com.mcmoddev.basemetals.entity.MaterialProjectile;
import com.mcmoddev.basemetals.entity.ModEntities;
import com.mcmoddev.basemetals.recipe.CrushingRecipe;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Server-lifecycle checks which cannot safely run in a plain JUnit JVM. */
@GameTestHolder(BaseMetals.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BaseMetalsGameTests {
    private static final String EMPTY = "empty";

    private BaseMetalsGameTests() {}

    @GameTest(template = EMPTY)
    public static void platesUseTheClickedFaceAndEdge(GameTestHelper helper) {
        PlateBlock plate = (PlateBlock) ModContent.blocksById().get("copper_plate").get();
        Player player = helper.makeMockPlayer();
        BlockPos position = helper.absolutePos(new BlockPos(1, 1, 1));

        BlockState centred = plate.getStateForPlacement(context(helper, player, position,
                new Vec3(position.getX() + 0.5D, position.getY() + 1.0D, position.getZ() + 0.5D), Direction.UP));
        BlockState easternEdge = plate.getStateForPlacement(context(helper, player, position,
                new Vec3(position.getX() + 0.95D, position.getY() + 1.0D, position.getZ() + 0.5D), Direction.UP));

        require(helper, centred != null && centred.getValue(PlateBlock.FACING) == Direction.UP,
                "A centred top-face placement did not face upward");
        // Legacy BlockMMDPlate rotated the plate inward from the clicked edge;
        // retaining that WEST result is part of old block-state compatibility.
        require(helper, easternEdge != null && easternEdge.getValue(PlateBlock.FACING) == Direction.WEST,
                "A top-face east-edge placement did not retain legacy west-facing orientation");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void humanDetectorIgnoresNonPlayers(GameTestHelper helper) {
        HumanDetectorBlock detector = (HumanDetectorBlock) ModContent.HUMAN_DETECTOR.get();
        BlockPos position = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.spawnWithNoFreeWill(EntityType.COW, 1.5F, 1.0F, 1.5F);
        require(helper, detector.getSignalStrength(helper.getLevel(), position) == 0,
                "Human detector responded to a non-player entity");

        Player player = helper.makeMockPlayer();
        player.moveTo(position.getX() + 0.5D, position.getY() + 0.1D, position.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(player);
        require(helper, detector.getSignalStrength(helper.getLevel(), position) == 15,
                "Human detector did not respond to a player");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void compatibilitySlabsStayDoubleAndHidden(GameTestHelper helper) {
        for (String id : ModContent.hiddenBlocks()) {
            BlockState state = ModContent.blocksById().get(id).get().defaultBlockState();
            require(helper, state.getValue(CompatibilityDoubleSlabBlock.TYPE) == SlabType.DOUBLE,
                    id + " did not default to a double slab");
            require(helper, !ForgeRegistries.ITEMS.containsKey(new ResourceLocation(BaseMetals.MOD_ID, id)),
                    id + " unexpectedly has a block item");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void everyFluidHasSourceFlowingBlockAndBucket(GameTestHelper helper) {
        ModContent.fluids().forEach((id, fluid) -> {
            require(helper, ForgeRegistries.FLUIDS.getKey(fluid.source().get()).equals(
                    new ResourceLocation(BaseMetals.MOD_ID, id)), id + " source fluid is not registered correctly");
            require(helper, ForgeRegistries.FLUIDS.getKey(fluid.flowing().get()).equals(
                    new ResourceLocation(BaseMetals.MOD_ID, "flowing_" + id)), id + " flowing fluid is not registered correctly");
            require(helper, ForgeRegistries.BLOCKS.getKey(fluid.block().get()).equals(
                    new ResourceLocation(BaseMetals.MOD_ID, id)), id + " fluid block is not registered correctly");
            require(helper, ForgeRegistries.ITEMS.getKey(fluid.bucket().get()).equals(
                    new ResourceLocation(BaseMetals.MOD_ID, id + "_bucket")), id + " bucket is not registered correctly");
        });
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void crushingRecipesLoadAndMatchOre(GameTestHelper helper) {
        ItemStack ore = ModContent.item("tin_ore").get().getDefaultInstance();
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(
                CrushingRecipe.TYPE.get(), new SimpleContainer(ore), helper.getLevel());
        require(helper, recipe.isPresent(), "No crushing recipe loaded for tin ore");
        require(helper, ForgeRegistries.ITEMS.getKey(recipe.orElseThrow().getResultItem().getItem()).equals(
                new ResourceLocation(BaseMetals.MOD_ID, "tin_powder")), "Tin ore crushed to the wrong output");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void projectilesRetainAmmunitionAcrossSaveAndSpawnData(GameTestHelper helper) {
        Player shooter = helper.makeMockPlayer();
        ItemStack ammunition = ModContent.item("starsteel_bolt").get().getDefaultInstance();
        MaterialProjectile original = new MaterialProjectile(ModEntities.CUSTOM_BOLT.get(), helper.getLevel(), shooter, ammunition);

        CompoundTag saved = new CompoundTag();
        original.addAdditionalSaveData(saved);
        MaterialProjectile loaded = new MaterialProjectile(ModEntities.CUSTOM_BOLT.get(), helper.getLevel());
        loaded.readAdditionalSaveData(saved);
        require(helper, ItemStack.isSameItemSameTags(ammunition, loaded.getItem()),
                "Projectile ammunition was not retained through save/load");

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            original.writeSpawnData(buffer);
            MaterialProjectile spawned = new MaterialProjectile(ModEntities.CUSTOM_BOLT.get(), helper.getLevel());
            spawned.readSpawnData(buffer);
            require(helper, ItemStack.isSameItemSameTags(ammunition, spawned.getItem()),
                    "Projectile ammunition was not retained through spawn data");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    private static BlockPlaceContext context(GameTestHelper helper, Player player, BlockPos position,
            Vec3 hit, Direction face) {
        return new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, ItemStack.EMPTY,
                new BlockHitResult(hit, face, position, false));
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
