package com.mcmoddev.basemetals.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mcmoddev.basemetals.BaseMetals;
import com.mcmoddev.basemetals.BaseMetalsEvents;
import com.mcmoddev.basemetals.ModTabs;
import com.mcmoddev.basemetals.entity.MaterialProjectile;
import com.mcmoddev.basemetals.entity.ModEntities;
import com.mcmoddev.basemetals.material.MaterialCatalogue;
import com.mcmoddev.basemetals.recipe.CrushingRecipe;
import com.mcmoddev.basemetals.recipe.PlateRepairRecipe;
import com.mcmoddev.basemetals.trade.BaseMetalsTrades;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
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
        MaterialCatalogue.ALL.stream().filter(material -> material.hasOre()).forEach(material ->
                assertCrushingRecipe(helper, ModContent.item(material.name() + "_ore").get().getDefaultInstance(),
                        material.name() + "_powder", 2));

        assertCrushingRecipe(helper, new ItemStack(Blocks.COAL_ORE), "coal_powder", 2);
        assertCrushingRecipe(helper, new ItemStack(Blocks.DEEPSLATE_COAL_ORE), "coal_powder", 2);
        assertCrushingRecipe(helper, new ItemStack(Blocks.COPPER_ORE), "copper_powder", 2);
        assertCrushingRecipe(helper, new ItemStack(Blocks.DEEPSLATE_COPPER_ORE), "copper_powder", 2);
        assertCrushingRecipe(helper, new ItemStack(Blocks.DIAMOND_ORE), "diamond_powder", 2);
        assertCrushingRecipe(helper, new ItemStack(Blocks.DEEPSLATE_DIAMOND_ORE), "diamond_powder", 2);
        assertCrushingRecipe(helper, new ItemStack(Blocks.EMERALD_ORE), "emerald_powder", 2);
        assertCrushingRecipe(helper, new ItemStack(Blocks.DEEPSLATE_EMERALD_ORE), "emerald_powder", 2);
        assertCrushingRecipe(helper, new ItemStack(Blocks.GOLD_ORE), "gold_powder", 2);
        assertCrushingRecipe(helper, new ItemStack(Blocks.DEEPSLATE_GOLD_ORE), "gold_powder", 2);
        assertCrushingRecipe(helper, new ItemStack(Blocks.NETHER_GOLD_ORE), "gold_powder", 2);
        assertCrushingRecipe(helper, new ItemStack(Blocks.IRON_ORE), "iron_powder", 2);
        assertCrushingRecipe(helper, new ItemStack(Blocks.DEEPSLATE_IRON_ORE), "iron_powder", 2);
        assertCrushingRecipe(helper, new ItemStack(Blocks.LAPIS_ORE), "minecraft:lapis_lazuli", 8);
        assertCrushingRecipe(helper, new ItemStack(Blocks.DEEPSLATE_LAPIS_ORE), "minecraft:lapis_lazuli", 8);
        assertCrushingRecipe(helper, new ItemStack(Blocks.REDSTONE_ORE), "minecraft:redstone", 8);
        assertCrushingRecipe(helper, new ItemStack(Blocks.DEEPSLATE_REDSTONE_ORE), "minecraft:redstone", 8);
        assertCrushingRecipe(helper, new ItemStack(Blocks.NETHER_QUARTZ_ORE), "minecraft:quartz", 2);
        assertCrushingRecipe(helper, new ItemStack(Blocks.ANCIENT_DEBRIS), "minecraft:netherite_scrap", 2);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void legacyFurnaceRecipesLoadAndRetainCountedOutputs(GameTestHelper helper) {
        MaterialCatalogue.ALL.forEach(material -> {
            assertSmeltingRecipeInTag(helper,
                    ModContent.item(material.name() + "_powder").get().getDefaultInstance(),
                    "forge:ingots/" + material.name(), 1);
            assertSmeltingRecipeInTag(helper,
                    ModContent.item(material.name() + "_smallpowder").get().getDefaultInstance(),
                    "forge:nuggets/" + material.name(), 1);
            if (material.isAlloy()) {
                assertSmeltingRecipeInTag(helper,
                        ModContent.item(material.name() + "_smallblend").get().getDefaultInstance(),
                        "forge:nuggets/" + material.name(), 1);
            }
        });

        Map<String, String> vanillaPowderOutputs = Map.of(
                "diamond", "minecraft:diamond",
                "emerald", "minecraft:emerald",
                "gold", "minecraft:gold_ingot",
                "iron", "minecraft:iron_ingot",
                "obsidian", "basemetals:obsidian_ingot",
                "quartz", "minecraft:quartz");
        vanillaPowderOutputs.forEach((material, output) ->
                assertSmeltingRecipe(helper, ModContent.item(material + "_powder").get().getDefaultInstance(),
                        output, 1));

        assertNoSmeltingRecipe(helper, ModContent.item("coal_powder").get().getDefaultInstance());
        assertNoSmeltingRecipe(helper, ModContent.item("charcoal_powder").get().getDefaultInstance());
        assertSmeltingRecipe(helper, ModContent.item("tin_crossbow").get().getDefaultInstance(),
                "basemetals:tin_ingot", 4);
        assertSmeltingRecipe(helper, ModContent.item("iron_crackhammer").get().getDefaultInstance(),
                "minecraft:iron_block", 1);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void stoneCrackhammerCraftsFromTheLegacyMiddleColumnPattern(GameTestHelper helper) {
        CraftingContainer crafting = craftingContainer(3, 3);
        crafting.setItem(1, new ItemStack(Blocks.STONE_BRICKS));
        crafting.setItem(4, new ItemStack(Items.STICK));
        crafting.setItem(7, new ItemStack(Items.STICK));

        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(
                RecipeType.CRAFTING, crafting, helper.getLevel());
        require(helper, recipe.isPresent(), "Stone bricks above two sticks matched no crafting recipe");
        if (recipe.isEmpty()) return;
        require(helper, recipe.orElseThrow().getId().equals(
                new ResourceLocation(BaseMetals.MOD_ID, "stone_crackhammer")),
                "The legacy stone crackhammer pattern matched the wrong recipe");
        ItemStack output = recipe.orElseThrow().assemble(crafting);
        require(helper, output.is(ModContent.item("stone_crackhammer").get()),
                "The legacy middle-column pattern returned the wrong item");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 250)
    public static void furnaceProcessesDustAndCountedLegacyRecovery(GameTestHelper helper) {
        BlockPos ironFurnacePosition = new BlockPos(1, 1, 1);
        BlockPos recoveryFurnacePosition = new BlockPos(2, 1, 1);
        helper.setBlock(ironFurnacePosition, Blocks.FURNACE);
        helper.setBlock(recoveryFurnacePosition, Blocks.FURNACE);

        AbstractFurnaceBlockEntity ironFurnace = (AbstractFurnaceBlockEntity)
                helper.getBlockEntity(ironFurnacePosition);
        ironFurnace.setItem(0, ModContent.item("iron_powder").get().getDefaultInstance());
        ironFurnace.setItem(1, new ItemStack(Items.COAL));

        AbstractFurnaceBlockEntity recoveryFurnace = (AbstractFurnaceBlockEntity)
                helper.getBlockEntity(recoveryFurnacePosition);
        recoveryFurnace.setItem(0, ModContent.item("tin_crossbow").get().getDefaultInstance());
        recoveryFurnace.setItem(1, new ItemStack(Items.COAL));

        helper.runAfterDelay(205, () -> {
            assertItemStack(helper, ironFurnace.getItem(2), "minecraft:iron_ingot", 1,
                    "Iron powder furnace output");
            assertItemStack(helper, recoveryFurnace.getItem(2), "basemetals:tin_ingot", 4,
                    "Counted legacy furnace output");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void crackhammerBreaksOreIntoFixedOutput(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack hammer = ModContent.item("stone_crackhammer").get().getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        BlockPos relativePosition = new BlockPos(1, 1, 1);
        BlockPos position = helper.absolutePos(relativePosition);
        helper.setBlock(relativePosition, Blocks.IRON_ORE);

        BlockState state = helper.getLevel().getBlockState(position);
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(helper.getLevel(), position, state, player);
        new BaseMetalsEvents().onBreak(event);

        require(helper, event.isCanceled(), "Crackhammer did not replace the vanilla ore break");
        require(helper, helper.getLevel().getBlockState(position).isAir(), "Crackhammer did not remove the ore");
        require(helper, hammer.getDamageValue() == 1, "Crackhammer was not damaged exactly once");
        requireSingleDrop(helper, position, null, "iron_powder", 2);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void crackhammerGroundCrushingUsesOneItemPerNormalUse(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack hammer = ModContent.item("stone_crackhammer").get().getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        BlockPos position = helper.absolutePos(new BlockPos(1, 1, 1));
        ItemEntity source = new ItemEntity(helper.getLevel(), position.getX() + 0.5D,
                position.getY() + 1.1D, position.getZ() + 0.5D,
                new ItemStack(Blocks.DEEPSLATE_COAL_ORE, 2));
        helper.getLevel().addFreshEntity(source);
        BlockHitResult hit = new BlockHitResult(
                new Vec3(position.getX() + 0.5D, position.getY() + 1.0D, position.getZ() + 0.5D),
                Direction.UP, position, false);

        InteractionResult result = hammer.getItem().useOn(
                new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, hammer, hit));

        require(helper, result.consumesAction(), "Ground crushing did not consume the interaction");
        require(helper, source.getItem().getCount() == 1, "Normal ground crushing did not consume exactly one ore");
        require(helper, hammer.getDamageValue() == 1, "Ground crushing did not damage the hammer exactly once");
        requireSingleDrop(helper, position.above(), source, "coal_powder", 2);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void scytheHarvestsHorizontalThreeByThreeAndPaysDurability(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack scythe = ModContent.item("iron_scythe").get().getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, scythe);
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                // Leaves are stable without a supporting block, unlike short
                // grass placed on the GameTest template's stone floor.
                helper.setBlock(new BlockPos(x, 1, z), Blocks.OAK_LEAVES);
            }
        }

        boolean handled = scythe.getItem().onBlockStartBreak(scythe,
                helper.absolutePos(new BlockPos(1, 1, 1)), player);

        require(helper, handled, "Scythe did not handle a tagged central plant");
        int remaining = 0;
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                if (!helper.getBlockState(new BlockPos(x, 1, z)).isAir()) remaining++;
            }
        }
        require(helper, remaining == 0,
                "Scythe left " + remaining + " of nine plants; durability=" + scythe.getDamageValue());
        require(helper, scythe.getDamageValue() == 9,
                "Scythe did not take one durability for each of nine harvested blocks");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void legacyToolAttributesAndFuelValuesSurviveThePort(GameTestHelper helper) {
        var bronze = MaterialCatalogue.get("bronze");
        ItemStack hoe = ModContent.item("bronze_hoe").get().getDefaultInstance();
        ItemStack scythe = ModContent.item("bronze_scythe").get().getDefaultInstance();
        ItemStack hammer = ModContent.item("bronze_crackhammer").get().getDefaultInstance();

        require(helper, attribute(hoe, Attributes.ATTACK_DAMAGE) == 0.0D,
                "Hoe added attack damage unlike 1.12");
        require(helper, close(attribute(hoe, Attributes.ATTACK_SPEED),
                bronze.baseAttackDamage() - 3.0F), "Hoe attack speed changed");
        require(helper, close(attribute(scythe, Attributes.ATTACK_DAMAGE), bronze.baseAttackDamage()),
                "Scythe attack damage changed");
        require(helper, attribute(scythe, Attributes.ATTACK_SPEED) == 0.0D,
                "Scythe attack speed changed");
        require(helper, close(attribute(hammer, Attributes.ATTACK_DAMAGE),
                bronze.crackhammerAttackDamage()), "Crackhammer attack damage changed");
        require(helper, attribute(hammer, Attributes.ATTACK_SPEED) == -3.5D,
                "Crackhammer attack speed changed");

        require(helper, burnTime("coal_powder") == 1600, "Coal powder no longer burns for one item");
        require(helper, burnTime("coal_smallpowder") == 200, "Tiny coal dust has the wrong burn time");
        require(helper, burnTime("wood_gear") == 300, "Wood gear has the wrong burn time");
        require(helper, burnTime("charcoal_block") == 16000, "Charcoal block has the wrong burn time");
        MaterialCatalogue.ALL.stream().filter(material -> material.hasEquipment()).forEach(material -> {
            require(helper, burnTime(material.name() + "_powder") == 1600,
                    material.name() + " powder lost its legacy fuel value");
            require(helper, burnTime(material.name() + "_nugget") == 200,
                    material.name() + " nugget lost its legacy fuel value");
            require(helper, burnTime(material.name() + "_smallpowder") == 200,
                    material.name() + " small powder lost its legacy fuel value");
        });
        for (String id : List.of(
                "mercury_nugget", "mercury_powder", "mercury_smallpowder",
                "diamond_nugget", "diamond_powder", "diamond_smallpowder",
                "emerald_nugget", "emerald_powder", "emerald_smallpowder",
                "gold_powder", "gold_smallpowder",
                "iron_powder", "iron_smallpowder",
                "obsidian_nugget", "obsidian_powder", "obsidian_smallpowder",
                "quartz_nugget", "quartz_powder", "quartz_smallpowder",
                "redstone_smallpowder", "lapis_smallpowder")) {
            require(helper, burnTime(id) == -1, id + " is an ahistorical furnace fuel");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void crackhammerUsesLegacySpeedForEveryHarvestableCrushingInput(GameTestHelper helper) {
        ItemStack stoneHammer = ModContent.item("stone_crackhammer").get().getDefaultInstance();
        float expected = ((MaterialBacked) stoneHammer.getItem()).baseMetalsMaterial()
                .crackhammerDestroySpeed();
        require(helper, close(stoneHammer.getDestroySpeed(Blocks.GRAVEL.defaultBlockState()), expected),
                "Crushable gravel retained hand-speed mining");
        require(helper, close(stoneHammer.getDestroySpeed(Blocks.GLASS.defaultBlockState()), expected),
                "Crushable glass retained hand-speed mining");
        require(helper, close(stoneHammer.getDestroySpeed(Blocks.SUGAR_CANE.defaultBlockState()), expected),
                "Crushable sugar cane retained hand-speed mining");
        require(helper, stoneHammer.getDestroySpeed(Blocks.OBSIDIAN.defaultBlockState()) == 1.0F,
                "An under-tier crackhammer mined Obsidian at crackhammer speed");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void legacyBlockCapabilitiesSoundsAndQuartzInteractionSurvive(GameTestHelper helper) {
        MaterialCatalogue.ALL.stream().filter(material -> material.hasEquipment()).forEach(material ->
                require(helper, ModContent.blocksById().get(material.name() + "_block").get()
                        .defaultBlockState().is(BlockTags.BEACON_BASE_BLOCKS),
                        material.name() + " storage block is missing from beacon bases"));
        require(helper, ModContent.blocksById().get("charcoal_block").get().defaultBlockState()
                .is(BlockTags.BEACON_BASE_BLOCKS), "Charcoal block is missing from beacon bases");
        require(helper, !ModContent.blocksById().get("mercury").get().defaultBlockState()
                .is(BlockTags.BEACON_BASE_BLOCKS), "Molten Mercury became a beacon base");

        require(helper, ModContent.blocksById().get("bronze_block").get().defaultBlockState()
                .getSoundType() == SoundType.METAL, "Base-metal storage blocks use the wrong sound");
        require(helper, ModContent.blocksById().get("diamond_wall").get().defaultBlockState()
                .getSoundType() == SoundType.GLASS, "Gem decorations use the wrong sound");
        require(helper, ModContent.blocksById().get("charcoal_block").get().defaultBlockState()
                .getSoundType() == SoundType.SAND, "Charcoal block uses the wrong sound");
        require(helper, ModContent.blocksById().get("tin_ore").get().defaultBlockState()
                .getSoundType() == SoundType.STONE, "Ore blocks use the wrong sound");
        require(helper, ModContent.blocksById().get("steel_anvil").get().defaultBlockState()
                .getSoundType() == SoundType.METAL, "Steel anvil uses the wrong sound");
        require(helper, ModContent.blocksById().get("stone_anvil").get().defaultBlockState()
                .getSoundType() == SoundType.STONE, "Stone anvil uses the wrong sound");

        Player player = helper.makeMockPlayer();
        for (String form : List.of("door", "trapdoor")) {
            BlockPos relative = form.equals("door") ? new BlockPos(1, 1, 1) : new BlockPos(2, 1, 1);
            BlockPos position = helper.absolutePos(relative);
            helper.setBlock(relative, ModContent.blocksById().get("quartz_" + form).get());
            BlockState state = helper.getLevel().getBlockState(position);
            InteractionResult result = state.use(helper.getLevel(), player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(position), Direction.UP, position, false));
            require(helper, result.consumesAction(), "Quartz " + form + " cannot be opened by hand");
        }
        BlockPos diamondRelative = new BlockPos(3, 1, 1);
        BlockPos diamondPosition = helper.absolutePos(diamondRelative);
        helper.setBlock(diamondRelative, ModContent.blocksById().get("diamond_door").get());
        BlockState diamond = helper.getLevel().getBlockState(diamondPosition);
        require(helper, diamond.use(helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(diamondPosition), Direction.UP, diamondPosition, false))
                == InteractionResult.PASS, "Diamond door became hand-openable");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void smithsRetainTheLegacyMercuryIngotSale(GameTestHelper helper) {
        Int2ObjectOpenHashMap<List<ItemListing>> trades = new Int2ObjectOpenHashMap<>();
        for (int level = 1; level <= 5; level++) trades.put(level, new ArrayList<>());
        BaseMetalsTrades.add(new VillagerTradesEvent(trades, VillagerProfession.ARMORER));
        boolean found = trades.values().stream().flatMap(List::stream)
                .map(listing -> listing.getOffer(null, new Random(0L)))
                .anyMatch(offer -> offer != null
                        && offer.getResult().is(ModContent.item("mercury_ingot").get())
                        && offer.getResult().getCount() == 12);
        require(helper, found, "Armorer lost the legacy 12-Mercury-ingot sale");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void creativeTabsRetainTheirLegacyStarsteelIcons(GameTestHelper helper) {
        require(helper, ModTabs.BLOCKS.makeIcon().is(ModContent.item("starsteel_block").get()),
                "Blocks tab did not retain the Starsteel block icon");
        require(helper, ModTabs.ITEMS.makeIcon().is(ModContent.item("starsteel_gear").get()),
                "Items tab did not retain the Starsteel gear icon");
        require(helper, ModTabs.TOOLS.makeIcon().is(ModContent.item("starsteel_pickaxe").get()),
                "Tools tab did not retain the Starsteel pickaxe icon");
        require(helper, ModTabs.COMBAT.makeIcon().is(ModContent.item("starsteel_sword").get()),
                "Combat tab did not retain the Starsteel sword icon");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void plateRepairKeepsOnlyEnchantmentsAndResetsDamage(GameTestHelper helper) {
        var recipe = helper.getLevel().getRecipeManager().byKey(
                new ResourceLocation(BaseMetals.MOD_ID, "adamantine_chestplate_plate_repair"));
        require(helper, recipe.isPresent(), "Adamantine chestplate plate-repair recipe did not load");
        if (recipe.isEmpty()) return;

        CraftingContainer crafting = craftingContainer(2, 1);
        ItemStack damaged = ModContent.item("adamantine_chestplate").get().getDefaultInstance();
        damaged.setDamageValue(20);
        damaged.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 2);
        damaged.setHoverName(new TextComponent("Old name"));
        damaged.getOrCreateTag().putBoolean("LegacyCustomData", true);
        crafting.setItem(0, damaged);
        crafting.setItem(1, ModContent.item("adamantine_plate").get().getDefaultInstance());

        require(helper, recipe.orElseThrow() instanceof PlateRepairRecipe,
                "Plate-repair recipe loaded with the wrong implementation");
        if (!(recipe.orElseThrow() instanceof PlateRepairRecipe repair)) return;
        require(helper, repair.matches(crafting, helper.getLevel()),
                "Plate-repair recipe rejected a damaged item and matching plate");
        ItemStack output = repair.assemble(crafting);
        require(helper, output.is(ModContent.item("adamantine_chestplate").get()),
                "Plate repair returned the wrong armor item");
        require(helper, output.getDamageValue() == 0, "Plate repair did not fully restore durability");
        require(helper, EnchantmentHelper.getItemEnchantmentLevel(
                Enchantments.ALL_DAMAGE_PROTECTION, output) == 2,
                "Plate repair lost enchantments");
        require(helper, !output.hasCustomHoverName(), "Plate repair retained unrelated custom-name NBT");
        require(helper, !output.getOrCreateTag().contains("LegacyCustomData"),
                "Plate repair retained unrelated custom data");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void shieldUpgradeSupportsVanillaBitsPlatesAndKeepsOnlyEnchantments(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack shield = ModContent.item("copper_shield").get().getDefaultInstance();
        shield.setDamageValue(10);
        shield.enchant(Enchantments.UNBREAKING, 2);
        shield.setHoverName(new TextComponent("Old shield"));
        shield.getOrCreateTag().putBoolean("LegacyCustomData", true);
        AnvilUpdateEvent event = new AnvilUpdateEvent(shield,
                ModContent.item("iron_plate").get().getDefaultInstance(), "", 0, player);

        new BaseMetalsEvents().onShieldUpgrade(event);

        require(helper, event.getOutput().is(ModContent.item("iron_shield").get()),
                "Iron Vanilla Bits plate did not upgrade a softer shield");
        require(helper, event.getOutput().getDamageValue() == 0,
                "Shield upgrade did not produce a fresh shield");
        require(helper, EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING,
                event.getOutput()) == 2, "Shield upgrade lost enchantments");
        require(helper, !event.getOutput().hasCustomHoverName(),
                "Shield upgrade retained unrelated custom-name NBT");
        require(helper, !event.getOutput().getOrCreateTag().contains("LegacyCustomData"),
                "Shield upgrade retained unrelated custom data");
        require(helper, event.getMaterialCost() == 1, "Shield upgrade did not consume exactly one plate");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void bowsAndLegacyCrossbowsUseOnlyTheirOwnAmmunition(GameTestHelper helper) {
        MaterialItems.Bow bow = (MaterialItems.Bow) ModContent.item("tin_bow").get();
        MaterialItems.Crossbow crossbow = (MaterialItems.Crossbow) ModContent.item("tin_crossbow").get();
        ItemStack arrow = ModContent.item("tin_arrow").get().getDefaultInstance();
        ItemStack bolt = ModContent.item("tin_bolt").get().getDefaultInstance();
        require(helper, bow.getAllSupportedProjectiles().test(Items.ARROW.getDefaultInstance()),
                "Base Metals bow rejected a vanilla arrow");
        require(helper, bow.getAllSupportedProjectiles().test(arrow),
                "Base Metals bow rejected a Base Metals arrow");
        require(helper, !bow.getAllSupportedProjectiles().test(bolt),
                "Base Metals bow accepted a bolt");
        require(helper, crossbow.getAllSupportedProjectiles().test(bolt),
                "Legacy crossbow rejected a Base Metals bolt");
        require(helper, !crossbow.getAllSupportedProjectiles().test(arrow),
                "Legacy crossbow accepted an arrow");

        ServerPlayer player = survivalPlayer(helper);
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        ItemStack weapon = crossbow.getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        player.getInventory().setItem(1, bolt.copy());
        player.startUsingItem(InteractionHand.MAIN_HAND);
        crossbow.releaseUsing(weapon, helper.getLevel(), player, crossbow.getUseDuration(weapon) - 20);

        // Assert synchronously: the GameTest harness may purge fast projectiles
        // after they leave its deliberately tiny empty structure.
        var projectiles = new ArrayList<MaterialProjectile>();
        helper.getLevel().getAllEntities().forEach(entity -> {
            if (entity instanceof MaterialProjectile projectile
                    && projectile.getOwner() == player
                    && projectile.getItem().is(ModContent.item("tin_bolt").get())) {
                projectiles.add(projectile);
            }
        });
        require(helper, projectiles.size() == 1,
                "Legacy crossbow projectile count=" + projectiles.size()
                        + ", ammo=" + player.getInventory().getItem(1).getCount()
                        + ", damage=" + weapon.getDamageValue());
        if (!projectiles.isEmpty()) {
            require(helper, projectiles.get(0).getItem().is(ModContent.item("tin_bolt").get()),
                    "Legacy crossbow projectile did not retain its bolt item");
        }
        require(helper, player.getInventory().getItem(1).isEmpty(),
                "Legacy crossbow did not consume a normal bolt");
        require(helper, weapon.getDamageValue() == 1,
                "Legacy crossbow did not take exactly one durability");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void vanillaBitsEquipmentUsesItsActualMaterialForAnvilRepair(GameTestHelper helper) {
        ItemStack diamondBow = ModContent.item("diamond_bow").get().getDefaultInstance();
        ItemStack emeraldSword = ModContent.item("emerald_sword").get().getDefaultInstance();
        ItemStack quartzArmor = ModContent.item("quartz_chestplate").get().getDefaultInstance();
        ItemStack stoneScythe = ModContent.item("stone_scythe").get().getDefaultInstance();
        ItemStack woodScythe = ModContent.item("wood_scythe").get().getDefaultInstance();

        require(helper, diamondBow.getItem().isValidRepairItem(diamondBow, new ItemStack(Items.DIAMOND)),
                "Diamond bow rejected a diamond repair ingredient");
        require(helper, emeraldSword.getItem().isValidRepairItem(emeraldSword, new ItemStack(Items.EMERALD)),
                "Emerald sword rejected an emerald repair ingredient");
        require(helper, quartzArmor.getItem().isValidRepairItem(quartzArmor, new ItemStack(Items.QUARTZ)),
                "Quartz armor rejected a quartz repair ingredient");
        require(helper, stoneScythe.getItem().isValidRepairItem(stoneScythe, new ItemStack(Blocks.STONE)),
                "Stone scythe rejected a stone repair ingredient");
        require(helper, woodScythe.getItem().isValidRepairItem(woodScythe, new ItemStack(Blocks.OAK_PLANKS)),
                "Wood scythe rejected a plank repair ingredient");
        require(helper, !diamondBow.getItem().isValidRepairItem(diamondBow, new ItemStack(Items.IRON_INGOT)),
                "Diamond bow accepted an unrelated ingot");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void armorToolEffectsAndStarsteelRegenerationAreServerAuthoritative(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        equip(player, "adamantine");
        player.tickCount = 20;
        BaseMetalsEvents events = new BaseMetalsEvents();
        events.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
        require(helper, player.getEffect(MobEffects.DAMAGE_RESISTANCE) != null
                && player.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() == 1,
                "Four Adamantine pieces did not grant Resistance II");
        require(helper, player.getEffect(MobEffects.DAMAGE_RESISTANCE).getDuration() == 45,
                "Armor effects linger far beyond the legacy refresh window");

        ItemStack starsteel = ModContent.item("starsteel_pickaxe").get().getDefaultInstance();
        starsteel.setDamageValue(5);
        ItemStack armor = ModContent.item("starsteel_helmet").get().getDefaultInstance();
        armor.setDamageValue(5);
        player.setItemInHand(InteractionHand.MAIN_HAND, starsteel);
        player.setItemInHand(InteractionHand.OFF_HAND, armor);
        player.tickCount = 200;
        events.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
        require(helper, starsteel.getDamageValue() == 4,
                "Held Starsteel tool did not regenerate one durability");
        require(helper, armor.getDamageValue() == 5,
                "Starsteel armor regenerated despite the armor exclusion");

        LivingEntity target = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, 2.0F, 1.0F, 2.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND,
                ModContent.item("adamantine_sword").get().getDefaultInstance());
        LivingHurtEvent hurt = new LivingHurtEvent(target, DamageSource.playerAttack(player), 3.0F);
        events.onLivingHurt(hurt);
        require(helper, hurt.getAmount() == 7.0F,
                "Adamantine melee bonus was not applied to a high-health target");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void remainingLegacyArmorEffectsUseTheCorrectPieceCounts(GameTestHelper helper) {
        BaseMetalsEvents events = new BaseMetalsEvents();

        ServerPlayer adamantine = survivalPlayer(helper);
        equip(adamantine, "adamantine");
        adamantine.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        tickEffects(events, adamantine);
        require(helper, effectAmplifier(adamantine, MobEffects.DAMAGE_RESISTANCE) == 0,
                "Three Adamantine pieces did not grant Resistance I");

        ServerPlayer lead = survivalPlayer(helper);
        equip(lead, "lead");
        tickEffects(events, lead);
        require(helper, effectAmplifier(lead, MobEffects.MOVEMENT_SLOWDOWN) == 1,
                "Four Lead pieces did not grant Slowness II");

        ServerPlayer coldiron = survivalPlayer(helper);
        equip(coldiron, "coldiron");
        tickEffects(events, coldiron);
        require(helper, effectAmplifier(coldiron, MobEffects.FIRE_RESISTANCE) == 0,
                "A full Cold Iron suit did not grant Fire Resistance I");

        ServerPlayer mithril = survivalPlayer(helper);
        equip(mithril, "mithril");
        mithril.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
        tickEffects(events, mithril);
        require(helper, !mithril.hasEffect(MobEffects.POISON),
                "A full Mithril suit did not remove a harmful effect");

        ServerPlayer starsteel = survivalPlayer(helper);
        equip(starsteel, "starsteel");
        tickEffects(events, starsteel);
        require(helper, effectAmplifier(starsteel, MobEffects.JUMP) == 3,
                "Four Starsteel pieces did not grant Jump Boost IV");
        require(helper, effectAmplifier(starsteel, MobEffects.MOVEMENT_SPEED) == 2,
                "Four Starsteel pieces did not grant Speed III");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void remainingLegacyMeleeEffectsUseTargetIdentity(GameTestHelper helper) {
        BaseMetalsEvents events = new BaseMetalsEvents();
        ServerPlayer player = survivalPlayer(helper);

        LivingEntity aquatic = helper.spawnWithNoFreeWill(EntityType.COD, 2.0F, 1.0F, 2.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND,
                ModContent.item("aquarium_sword").get().getDefaultInstance());
        LivingHurtEvent aquariumHit = new LivingHurtEvent(aquatic, DamageSource.playerAttack(player), 3.0F);
        events.onLivingHurt(aquariumHit);
        require(helper, aquariumHit.getAmount() == 7.0F,
                "Aquarium melee bonus did not use the target's water-breathing identity");

        LivingEntity blaze = helper.spawnWithNoFreeWill(EntityType.BLAZE, 3.0F, 1.0F, 2.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND,
                ModContent.item("coldiron_sword").get().getDefaultInstance());
        LivingHurtEvent coldIronHit = new LivingHurtEvent(blaze, DamageSource.playerAttack(player), 3.0F);
        events.onLivingHurt(coldIronHit);
        require(helper, coldIronHit.getAmount() == 6.0F,
                "Cold Iron melee bonus did not use the target's fire immunity");

        LivingEntity undead = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 4.0F, 1.0F, 2.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND,
                ModContent.item("mithril_sword").get().getDefaultInstance());
        LivingHurtEvent mithrilHit = new LivingHurtEvent(undead, DamageSource.playerAttack(player), 3.0F);
        events.onLivingHurt(mithrilHit);
        require(helper, effectAmplifier(undead, MobEffects.WITHER) == 3,
                "Mithril did not apply Wither IV to undead");
        require(helper, effectAmplifier(undead, MobEffects.BLINDNESS) == 1,
                "Mithril did not apply Blindness II to undead");
        require(helper, undead.getEffect(MobEffects.WITHER).getDuration() == 60
                && undead.getEffect(MobEffects.BLINDNESS).getDuration() == 60,
                "Mithril undead effects did not retain the 60-tick duration");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void eventBackedAdvancementsAwardForActualLegacyActions(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "basemetals-advancement-test"));
        player.setGameMode(GameType.SURVIVAL);
        BaseMetalsEvents events = new BaseMetalsEvents();
        events.onItemCrafted(new PlayerEvent.ItemCraftedEvent(player,
                ModContent.item("iron_crackhammer").get().getDefaultInstance(), new SimpleContainer(0)));
        events.onItemCrafted(new PlayerEvent.ItemCraftedEvent(player,
                ModContent.item("bronze_blend").get().getDefaultInstance(), new SimpleContainer(0)));
        events.onItemSmelted(new PlayerEvent.ItemSmeltedEvent(player,
                ModContent.item("bronze_ingot").get().getDefaultInstance()));

        for (String advancement : new String[] {"geologist", "metallurgy", "this_is_new", "bronze_maker"}) {
            var definition = player.getServer().getAdvancements().getAdvancement(
                    new ResourceLocation(BaseMetals.MOD_ID, advancement));
            require(helper, definition != null
                    && player.getAdvancements().getOrStartProgress(definition).isDone(),
                    "Legacy action did not award advancement " + advancement);
        }
        helper.succeed();
    }

    private static void assertCrushingRecipe(GameTestHelper helper, ItemStack input,
            String expectedItem, int expectedCount) {
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(
                CrushingRecipe.TYPE.get(), new SimpleContainer(input), helper.getLevel());
        require(helper, recipe.isPresent(), "No crushing recipe loaded for " + input);
        ItemStack result = recipe.orElseThrow().getResultItem();
        ResourceLocation expectedId = expectedItem.contains(":")
                ? new ResourceLocation(expectedItem)
                : new ResourceLocation(BaseMetals.MOD_ID, expectedItem);
        require(helper, ForgeRegistries.ITEMS.getKey(recipe.orElseThrow().getResultItem().getItem()).equals(
                expectedId), input + " crushed to the wrong output");
        require(helper, result.getCount() == expectedCount,
                input + " crushed to " + result.getCount() + " items instead of " + expectedCount);
    }

    private static void tickEffects(BaseMetalsEvents events, ServerPlayer player) {
        player.tickCount = 20;
        events.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
    }

    private static int effectAmplifier(LivingEntity entity, net.minecraft.world.effect.MobEffect effect) {
        MobEffectInstance instance = entity.getEffect(effect);
        return instance == null ? -1 : instance.getAmplifier();
    }

    private static void assertSmeltingRecipe(GameTestHelper helper, ItemStack input,
            String expectedItem, int expectedCount) {
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(
                RecipeType.SMELTING, new SimpleContainer(input), helper.getLevel());
        require(helper, recipe.isPresent(), "No furnace recipe loaded for " + input);
        if (recipe.isEmpty()) return;
        ItemStack result = recipe.orElseThrow().getResultItem();
        ResourceLocation actualItem = ForgeRegistries.ITEMS.getKey(result.getItem());
        require(helper, actualItem.equals(new ResourceLocation(expectedItem)),
                input + " smelted to " + actualItem + " instead of " + expectedItem);
        require(helper, result.getCount() == expectedCount,
                input + " smelted to " + result.getCount() + " items instead of " + expectedCount);
    }

    private static void assertSmeltingRecipeInTag(GameTestHelper helper, ItemStack input,
            String expectedTag, int expectedCount) {
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(
                RecipeType.SMELTING, new SimpleContainer(input), helper.getLevel());
        require(helper, recipe.isPresent(), "No furnace recipe loaded for " + input);
        if (recipe.isEmpty()) return;
        ItemStack result = recipe.orElseThrow().getResultItem();
        TagKey<net.minecraft.world.item.Item> tag = TagKey.create(
                Registry.ITEM_REGISTRY, new ResourceLocation(expectedTag));
        ResourceLocation actualItem = ForgeRegistries.ITEMS.getKey(result.getItem());
        require(helper, result.is(tag),
                input + " smelted to " + actualItem + ", which is outside #" + expectedTag);
        require(helper, result.getCount() == expectedCount,
                input + " smelted to " + result.getCount() + " items instead of " + expectedCount);
    }

    private static void assertNoSmeltingRecipe(GameTestHelper helper, ItemStack input) {
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(
                RecipeType.SMELTING, new SimpleContainer(input), helper.getLevel());
        require(helper, recipe.isEmpty(), input + " unexpectedly has a furnace recipe");
    }

    private static void assertItemStack(GameTestHelper helper, ItemStack stack, String expectedItem,
            int expectedCount, String description) {
        require(helper, ForgeRegistries.ITEMS.getKey(stack.getItem()).equals(
                new ResourceLocation(expectedItem)), description + " used the wrong item");
        require(helper, stack.getCount() == expectedCount,
                description + " contained " + stack.getCount() + " items instead of " + expectedCount);
    }

    private static void requireSingleDrop(GameTestHelper helper, BlockPos position, ItemEntity excluded,
            String expectedItem, int expectedCount) {
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(position).inflate(0.25D),
                entity -> entity != excluded);
        require(helper, drops.size() == 1, "Expected one crushing output entity but found " + drops.size());
        if (drops.size() != 1) return;
        ItemStack output = drops.get(0).getItem();
        require(helper, ForgeRegistries.ITEMS.getKey(output.getItem()).equals(
                new ResourceLocation(BaseMetals.MOD_ID, expectedItem)), "Crushing produced the wrong item");
        require(helper, output.getCount() == expectedCount,
                "Crushing produced " + output.getCount() + " items instead of " + expectedCount);
    }

    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "basemetals-crushing-test"));
        player.setGameMode(GameType.SURVIVAL);
        return player;
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

    private static CraftingContainer craftingContainer(int width, int height) {
        return new CraftingContainer(new AbstractContainerMenu(null, -1) {
            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        }, width, height);
    }

    private static double attribute(ItemStack stack, Attribute attribute) {
        return stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(attribute).stream()
                .mapToDouble(AttributeModifier::getAmount).sum();
    }

    private static int burnTime(String item) {
        ItemStack stack = ModContent.item(item).get().getDefaultInstance();
        return stack.getBurnTime(RecipeType.SMELTING);
    }

    private static boolean close(double left, double right) {
        return Math.abs(left - right) < 0.0001D;
    }

    private static void equip(ServerPlayer player, String material) {
        player.setItemSlot(EquipmentSlot.HEAD,
                ModContent.item(material + "_helmet").get().getDefaultInstance());
        player.setItemSlot(EquipmentSlot.CHEST,
                ModContent.item(material + "_chestplate").get().getDefaultInstance());
        player.setItemSlot(EquipmentSlot.LEGS,
                ModContent.item(material + "_leggings").get().getDefaultInstance());
        player.setItemSlot(EquipmentSlot.FEET,
                ModContent.item(material + "_boots").get().getDefaultInstance());
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
