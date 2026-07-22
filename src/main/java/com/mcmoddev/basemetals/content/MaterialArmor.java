package com.mcmoddev.basemetals.content;

import java.util.function.Supplier;

import com.mcmoddev.basemetals.material.MaterialDefinition;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

public final class MaterialArmor implements ArmorMaterial {
    private static final int[] DURABILITY = {13, 15, 16, 11};
    private final MaterialDefinition material;
    private final Supplier<Ingredient> repair;

    public MaterialArmor(MaterialDefinition material) {
        this.material = material;
        this.repair = () -> Ingredient.of(ItemTags.create(
                new ResourceLocation("forge", "ingots/" + material.name())));
    }

    public MaterialDefinition material() { return material; }
    @Override public int getDurabilityForSlot(EquipmentSlot slot) {
        return DURABILITY[slot.getIndex()] * material.armorDurabilityFactor();
    }
    @Override public int getDefenseForSlot(EquipmentSlot slot) { return material.armorProtection(slot); }
    @Override public int getEnchantmentValue() { return material.enchantability(); }
    @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_IRON; }
    @Override public Ingredient getRepairIngredient() { return repair.get(); }
    @Override public String getName() { return "basemetals:" + material.name(); }
    @Override public float getToughness() { return Math.max(0.0F, material.toolLevel() - 2.0F); }
    @Override public float getKnockbackResistance() { return 0.0F; }
}
