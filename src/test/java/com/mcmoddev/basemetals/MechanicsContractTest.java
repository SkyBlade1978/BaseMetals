package com.mcmoddev.basemetals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.mcmoddev.basemetals.material.MaterialCatalogue;
import com.mcmoddev.basemetals.material.MaterialDefinition;
import org.junit.jupiter.api.Test;

class MechanicsContractTest {
    @Test
    void historicalAliasesResolveToTheirPreservedIds() {
        assertEquals("mercury", MissingMappings.blockTargetPath("liquid_mercury"));
        assertEquals("mercury", MissingMappings.fluidTargetPath("liquid_mercury"));
        assertEquals("coal_powder", MissingMappings.itemTargetPath("carbon_powder"));
        assertEquals("double_diamond_slab", MissingMappings.blockTargetPath("double_diamond_slab"));
        assertEquals("human_detector", MissingMappings.blockTargetPath("human_detector"));
        assertEquals("emerald_bow", MissingMappings.itemTargetPath("emerald_bow"));
    }

    @Test
    void shieldUpgradeCostUsesHardnessMagicAndEnchantments() {
        MaterialDefinition copper = MaterialCatalogue.get("copper");
        MaterialDefinition silver = MaterialCatalogue.get("silver");
        MaterialDefinition starsteel = MaterialCatalogue.get("starsteel");
        assertEquals(5, BaseMetalsEvents.shieldUpgradeCost(copper, silver, 0));
        assertEquals(35, BaseMetalsEvents.shieldUpgradeCost(copper, silver, 5));
        assertEquals(90, BaseMetalsEvents.shieldUpgradeCost(copper, starsteel, 5));
    }
}
