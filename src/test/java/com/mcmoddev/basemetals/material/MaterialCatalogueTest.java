package com.mcmoddev.basemetals.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.world.entity.EquipmentSlot;

class MaterialCatalogueTest {
    @Test
    void containsTheHistoricalTwentyTwoMaterials() {
        assertEquals(22, MaterialCatalogue.ALL.size());
        assertEquals(13, MaterialCatalogue.ALL.stream().filter(MaterialDefinition::hasOre).count());
        assertTrue(MaterialCatalogue.BY_NAME.containsKey("mercury"));
        assertFalse(MaterialCatalogue.get("mercury").hasEquipment());
    }

    @Test
    void derivesHistoricalAdamantineStatisticsWithinModernBounds() {
        MaterialDefinition adamantine = MaterialCatalogue.get("adamantine");
        assertEquals(24.0F, adamantine.blockHardness());
        assertEquals(6.0F, adamantine.oreHardness());
        assertEquals(2000.0F, adamantine.blastResistance());
        assertEquals(4, adamantine.toolLevel());
        assertEquals(3200, adamantine.toolDurability());
        assertEquals(200, adamantine.armorDurabilityFactor());
        assertEquals(0, adamantine.enchantability());
        assertEquals(2, adamantine.armorProtection(EquipmentSlot.HEAD));
        assertEquals(8, adamantine.armorProtection(EquipmentSlot.CHEST));
        assertEquals(7, adamantine.armorProtection(EquipmentSlot.LEGS));
        assertEquals(3, adamantine.armorProtection(EquipmentSlot.FEET));
    }

    @Test
    void processingFormsRemainLimitedToTheEightLegacyFamilies() {
        assertEquals(8, MaterialCatalogue.ALL.stream().filter(MaterialDefinition::processingForms).count());
        assertTrue(MaterialCatalogue.get("zinc").processingForms());
        assertFalse(MaterialCatalogue.get("copper").processingForms());
    }

    @Test
    void everyDerivedValueUsesTheHistoricalFormulaAndModernBounds() {
        for (MaterialDefinition material : MaterialCatalogue.ALL) {
            assertEquals((float) (2.0D * material.hardness()), material.blockHardness(), material.name());
            assertEquals((float) (0.5D * material.hardness()), material.oreHardness(), material.name());
            float resistance = material.name().equals("adamantine") || material.name().equals("starsteel")
                    ? 2000.0F : (float) (2.5D * material.strength());
            assertEquals(resistance, material.blastResistance(), material.name());
            assertEquals(Math.max(0, Math.min(4, (int) (material.hardness() / 3.0D))),
                    material.toolLevel(), material.name());
            assertEquals(Math.max(1, (int) (32.0D * material.strength())),
                    material.toolDurability(), material.name());
            assertEquals((float) material.hardness(), material.toolEfficiency(), material.name());
            assertEquals(Math.max(0, (int) (2.5D * material.magic())),
                    material.enchantability(), material.name());
            assertEquals(Math.max(1, (int) (2.0D * material.strength())),
                    material.armorDurabilityFactor(), material.name());
            for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
                assertTrue(material.armorProtection(slot) >= 0 && material.armorProtection(slot) <= 30,
                        material.name() + " " + slot);
            }
        }
        assertEquals(0.9F, MaterialCatalogue.get("brass").baseAttackDamage());
        assertEquals(2.0F, MaterialCatalogue.get("bronze").baseAttackDamage());
    }
}
