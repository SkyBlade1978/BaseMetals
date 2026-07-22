package com.mcmoddev.basemetals.content;

import java.util.function.Supplier;

import com.mcmoddev.basemetals.BaseMetals;
import com.mcmoddev.basemetals.material.MaterialDefinition;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.Registry;

public final class MaterialTier implements Tier {
    private final MaterialDefinition material;
    private final Supplier<Ingredient> repair;

    public MaterialTier(MaterialDefinition material) {
        this.material = material;
        TagKey<Item> tag = ItemTags.create(new ResourceLocation("forge", "ingots/" + material.name()));
        this.repair = () -> Ingredient.of(tag);
    }

    public MaterialDefinition material() {
        return material;
    }

    @Override public int getUses() { return material.toolDurability(); }
    @Override public float getSpeed() { return material.toolEfficiency(); }
    @Override public float getAttackDamageBonus() { return material.baseAttackDamage(); }
    @Override public int getLevel() { return material.toolLevel(); }
    @Override public int getEnchantmentValue() { return material.enchantability(); }
    @Override public Ingredient getRepairIngredient() { return repair.get(); }
}
