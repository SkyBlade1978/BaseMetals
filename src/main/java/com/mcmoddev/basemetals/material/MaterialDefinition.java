package com.mcmoddev.basemetals.material;

import java.util.Objects;

import net.minecraft.world.entity.EquipmentSlot;

/** Immutable source of truth for every Base Metals material. */
public record MaterialDefinition(
        String name,
        Kind kind,
        double hardness,
        double strength,
        double magic,
        int colour,
        boolean processingForms) {

    public enum Kind {
        ORE, RARE_ORE, ALLOY, RARE_ALLOY, MERCURY;

        public boolean hasOre() {
            return this == ORE || this == RARE_ORE || this == MERCURY;
        }

        public boolean isAlloy() {
            return this == ALLOY || this == RARE_ALLOY;
        }
    }

    public MaterialDefinition {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(kind, "kind");
        if (!name.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid material name: " + name);
        }
        if (hardness <= 0 || strength <= 0 || magic < 0) {
            throw new IllegalArgumentException("Invalid material statistics for " + name);
        }
    }

    public boolean hasOre() {
        return kind.hasOre();
    }

    public boolean isAlloy() {
        return kind.isAlloy();
    }

    public boolean hasEquipment() {
        return kind != Kind.MERCURY;
    }

    public float blockHardness() {
        return (float) (2.0D * hardness);
    }

    public float oreHardness() {
        return (float) (0.5D * hardness);
    }

    public float blastResistance() {
        return name.equals("adamantine") || name.equals("starsteel")
                ? 2000.0F
                : (float) (2.5D * strength);
    }

    public int toolLevel() {
        return Math.max(0, Math.min(4, (int) (hardness / 3.0D)));
    }

    public int requiredHarvestLevel() {
        return Math.max(-1, Math.min(3, (int) ((0.9D * hardness) / 3.0D)));
    }

    public int toolDurability() {
        return Math.max(1, (int) (32.0D * strength));
    }

    public float toolEfficiency() {
        return (float) hardness;
    }

    public float baseAttackDamage() {
        return Math.round((float) (2.5D * hardness)) / 10.0F;
    }

    public int enchantability() {
        return Math.max(0, (int) (2.5D * magic));
    }

    public int armorDurabilityFactor() {
        return Math.max(1, (int) (2.0D * strength));
    }

    public int armorProtection(EquipmentSlot slot) {
        double total = (1.25D * hardness) + 5.0D;
        double fraction = switch (slot) {
            case HEAD -> 0.10D;
            case CHEST -> 0.40D;
            case LEGS -> 0.35D;
            case FEET -> 0.15D;
            default -> 0.0D;
        };
        // Vanilla stores this as a non-negative int; keep the historical
        // distribution but prevent pathological values from corrupting data.
        return Math.max(0, Math.min(30, (int) Math.round(total * fraction)));
    }

    public int horseArmorProtection() {
        return Math.max(0, Math.min(30, (int) ((hardness / 10.0D) * 11.0D)));
    }

    public float oreSmeltingExperience() {
        return Math.max(0.1F, (float) (0.1D * magic));
    }
}
