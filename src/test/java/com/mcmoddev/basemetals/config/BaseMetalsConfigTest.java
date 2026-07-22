package com.mcmoddev.basemetals.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BaseMetalsConfigTest {
    @Test
    void exposesOnlyTheFourDefaultTrueGameplaySwitches() {
        assertEquals(4, BaseMetalsConfig.SPEC.getValues().size());
        assertTrue(BaseMetalsConfig.SPECIAL_EFFECTS.get());
        assertTrue(BaseMetalsConfig.STARSTEEL_REGENERATION.get());
        assertTrue(BaseMetalsConfig.MERCURY_EFFECTS.get());
        assertTrue(BaseMetalsConfig.VILLAGER_TRADES.get());
    }
}
