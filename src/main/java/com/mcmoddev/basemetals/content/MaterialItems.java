package com.mcmoddev.basemetals.content;

import com.mcmoddev.basemetals.material.MaterialDefinition;
import com.mcmoddev.basemetals.BaseMetals;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.HorseArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

public final class MaterialItems {
    private MaterialItems() {}

    public static final class Basic extends Item implements MaterialBacked {
        private final MaterialDefinition material;
        public Basic(MaterialDefinition material, Properties properties) { super(properties); this.material = material; }
        @Override public MaterialDefinition baseMetalsMaterial() { return material; }
    }

    public static final class Pickaxe extends PickaxeItem implements MaterialBacked {
        private final MaterialDefinition material;
        public Pickaxe(MaterialDefinition material, Properties properties) {
            super(new MaterialTier(material), 1, -2.8F, properties); this.material = material;
        }
        @Override public MaterialDefinition baseMetalsMaterial() { return material; }
    }

    public static final class Axe extends AxeItem implements MaterialBacked {
        private final MaterialDefinition material;
        public Axe(MaterialDefinition material, Properties properties) {
            super(new MaterialTier(material), 5.0F, -3.0F, properties); this.material = material;
        }
        @Override public MaterialDefinition baseMetalsMaterial() { return material; }
    }

    public static final class Shovel extends ShovelItem implements MaterialBacked {
        private final MaterialDefinition material;
        public Shovel(MaterialDefinition material, Properties properties) {
            super(new MaterialTier(material), 1.5F, -3.0F, properties); this.material = material;
        }
        @Override public MaterialDefinition baseMetalsMaterial() { return material; }
    }

    public static final class Hoe extends HoeItem implements MaterialBacked {
        private final MaterialDefinition material;
        public Hoe(MaterialDefinition material, Properties properties) {
            super(new MaterialTier(material), -2, -1.0F, properties); this.material = material;
        }
        @Override public MaterialDefinition baseMetalsMaterial() { return material; }
    }

    public static class Sword extends SwordItem implements MaterialBacked {
        private final MaterialDefinition material;
        public Sword(MaterialDefinition material, Properties properties) {
            super(new MaterialTier(material), 3, -2.4F, properties); this.material = material;
        }
        @Override public MaterialDefinition baseMetalsMaterial() { return material; }
    }

    public static final class Armor extends ArmorItem implements MaterialBacked {
        private final MaterialDefinition material;
        public Armor(MaterialDefinition material, EquipmentSlot slot, Properties properties) {
            super(new MaterialArmor(material), slot, properties); this.material = material;
        }
        @Override public MaterialDefinition baseMetalsMaterial() { return material; }
    }

    public static final class HorseArmor extends HorseArmorItem implements MaterialBacked {
        private final MaterialDefinition material;
        public HorseArmor(MaterialDefinition material, Properties properties) {
            super(material.horseArmorProtection(), new ResourceLocation(BaseMetals.MOD_ID,
                    "textures/entity/horse/armor/horse_armor_" + material.name() + ".png"), properties);
            this.material = material;
        }
        @Override public MaterialDefinition baseMetalsMaterial() { return material; }
    }

    public static final class Shears extends ShearsItem implements MaterialBacked {
        private final MaterialDefinition material;
        public Shears(MaterialDefinition material, Properties properties) { super(properties); this.material = material; }
        @Override public MaterialDefinition baseMetalsMaterial() { return material; }
    }

    public static class Shield extends ShieldItem implements MaterialBacked {
        private final MaterialDefinition material;
        public Shield(MaterialDefinition material, Properties properties) { super(properties); this.material = material; }
        @Override public MaterialDefinition baseMetalsMaterial() { return material; }
    }

    public static class Bow extends BowItem implements MaterialBacked {
        private final MaterialDefinition material;
        public Bow(MaterialDefinition material, Properties properties) { super(properties); this.material = material; }
        @Override public MaterialDefinition baseMetalsMaterial() { return material; }
        @Override public Predicate<ItemStack> getAllSupportedProjectiles() {
            return stack -> stack.getItem() instanceof BaseMetalAmmoItem ammo
                    && ammo.kind() == BaseMetalAmmoItem.Kind.ARROW;
        }
    }

    public static final class Crossbow extends BowItem implements MaterialBacked {
        private final MaterialDefinition material;
        public Crossbow(MaterialDefinition material, Properties properties) { super(properties); this.material = material; }
        @Override public MaterialDefinition baseMetalsMaterial() { return material; }
        @Override public Predicate<ItemStack> getAllSupportedProjectiles() {
            return stack -> stack.getItem() instanceof BaseMetalAmmoItem ammo
                    && ammo.kind() == BaseMetalAmmoItem.Kind.BOLT;
        }
    }

    public static final class FishingRod extends FishingRodItem implements MaterialBacked {
        private final MaterialDefinition material;
        public FishingRod(MaterialDefinition material, Properties properties) { super(properties); this.material = material; }
        @Override public MaterialDefinition baseMetalsMaterial() { return material; }
    }
}
