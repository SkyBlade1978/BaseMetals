package zone.moddev.mc.basemetals;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import zone.moddev.mc.basemetals.client.ClientSetup;
import zone.moddev.mc.basemetals.config.BaseMetalsConfig;
import zone.moddev.mc.basemetals.content.ModContent;
import zone.moddev.mc.basemetals.entity.ModEntities;
import zone.moddev.mc.basemetals.migration.LegacyWorldDataHook;
import zone.moddev.mc.basemetals.recipe.CrushingRecipe;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(BaseMetals.MOD_ID)
public final class BaseMetals {
    public static final String MOD_ID = "basemetals";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public BaseMetals() {
        LegacyWorldDataHook.register();
        ModContent.initializeFluids();
        CrushingRecipe.register();
        ModEntities.initialize();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BaseMetalsConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(new BaseMetalsEvents());
        DistExecutor.runWhenOn(Dist.CLIENT, () -> ClientSetup::register);
    }
}
