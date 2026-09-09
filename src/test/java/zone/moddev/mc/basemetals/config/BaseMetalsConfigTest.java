package zone.moddev.mc.basemetals.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import com.electronwill.nightconfig.core.CommentedConfig;
import org.junit.jupiter.api.Test;

class BaseMetalsConfigTest {
    @Test
    void exposesOnlyTheFourDefaultTrueGameplaySwitches() {
        int switches = 0;
        for (Field field : BaseMetalsConfig.class.getDeclaredFields()) {
            if (field.getType() == net.minecraftforge.common.ForgeConfigSpec.BooleanValue.class) switches++;
        }
        assertEquals(4, switches);
        CommentedConfig defaults = CommentedConfig.inMemory();
        BaseMetalsConfig.SPEC.correct(defaults);
        BaseMetalsConfig.SPEC.setConfig(defaults);
        assertTrue(BaseMetalsConfig.SPECIAL_EFFECTS.get());
        assertTrue(BaseMetalsConfig.STARSTEEL_REGENERATION.get());
        assertTrue(BaseMetalsConfig.MERCURY_EFFECTS.get());
        assertTrue(BaseMetalsConfig.VILLAGER_TRADES.get());
    }
}
