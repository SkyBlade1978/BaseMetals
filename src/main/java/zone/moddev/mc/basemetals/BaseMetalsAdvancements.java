package zone.moddev.mc.basemetals;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import zone.moddev.mc.basemetals.content.CrackhammerItem;
import zone.moddev.mc.basemetals.content.MaterialBacked;
import zone.moddev.mc.basemetals.content.MaterialItems;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;

final class BaseMetalsAdvancements {
    private static final String CRITERION = "event";
    private static final Tag<Block> STORAGE_BLOCKS = new BlockTags.Wrapper(
            new ResourceLocation("forge", "storage_blocks"));
    private static final Map<String, String> ALLOY_ADVANCEMENTS;

    static {
        Map<String, String> values = new LinkedHashMap<String, String>();
        values.put("aquarium", "aquarium_maker");
        values.put("brass", "brass_maker");
        values.put("bronze", "bronze_maker");
        values.put("cupronickel", "cupronickel_maker");
        values.put("electrum", "electrum_maker");
        values.put("invar", "invar_maker");
        values.put("mithril", "mithril_maker");
        values.put("pewter", "pewter_maker");
        values.put("steel", "steel_maker");
        ALLOY_ADVANCEMENTS = Collections.unmodifiableMap(values);
    }

    private BaseMetalsAdvancements() {}

    static void onCrafted(EntityPlayerMP player, ItemStack result) {
        if (result.getItem() instanceof CrackhammerItem) award(player, "geologist");
        ResourceLocation id = result.getItem().getRegistryName();
        if (baseMetals(id) && (id.getPath().endsWith("_blend") || id.getPath().endsWith("_smallblend"))) {
            award(player, "metallurgy");
        }
    }

    static void onSmelted(EntityPlayerMP player, ItemStack result) {
        ResourceLocation id = result.getItem().getRegistryName();
        if (!baseMetals(id) || !id.getPath().endsWith("_ingot")) return;
        award(player, "this_is_new");
        String material = id.getPath().substring(0, id.getPath().length() - "_ingot".length());
        String advancement = ALLOY_ADVANCEMENTS.get(material);
        if (advancement != null) award(player, advancement);
    }

    static void onPlaced(EntityPlayerMP player, IBlockState state) {
        ResourceLocation id = state.getBlock().getRegistryName();
        if (baseMetals(id) && state.isIn(STORAGE_BLOCKS)) award(player, "blocktastic");
    }

    static void onEquipment(EntityPlayerMP player) {
        if (fullArmor(player, "coldiron") && mainHandSword(player, "coldiron")) award(player, "demon_slayer");
        if (fullArmor(player, "mithril") && mainHandSword(player, "mithril")) award(player, "angel_of_death");
        if (fullArmor(player, "aquarium") && player.isInWater()) award(player, "scuba_diver");
        if (fullArmor(player, "adamantine")) award(player, "juggernaut");
        if (material(player.getItemStackFromSlot(EntityEquipmentSlot.FEET), "starsteel")) award(player, "moon_boots");
    }

    private static boolean fullArmor(EntityPlayerMP player, String material) {
        EntityEquipmentSlot[] slots = { EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST,
                EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET };
        for (EntityEquipmentSlot slot : slots) {
            ItemStack stack = player.getItemStackFromSlot(slot);
            if (!(stack.getItem() instanceof ItemArmor) || !material(stack, material)) return false;
        }
        return true;
    }

    private static boolean mainHandSword(EntityPlayerMP player, String material) {
        ItemStack stack = player.getHeldItemMainhand();
        return stack.getItem() instanceof MaterialItems.Sword && material(stack, material);
    }

    private static boolean material(ItemStack stack, String expected) {
        return stack.getItem() instanceof MaterialBacked
                && ((MaterialBacked) stack.getItem()).baseMetalsMaterial().name().equals(expected);
    }

    private static boolean baseMetals(ResourceLocation id) {
        return id != null && BaseMetals.MOD_ID.equals(id.getNamespace());
    }

    static boolean award(EntityPlayerMP player, String id) {
        Advancement advancement = player.getServer().getAdvancementManager().getAdvancement(
                new ResourceLocation(BaseMetals.MOD_ID, id));
        if (advancement == null) return false;
        AdvancementProgress progress = player.getAdvancements().getProgress(advancement);
        return !progress.isDone() && player.getAdvancements().grantCriterion(advancement, CRITERION);
    }
}
