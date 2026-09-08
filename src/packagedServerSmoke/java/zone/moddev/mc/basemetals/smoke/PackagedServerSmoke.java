package zone.moddev.mc.basemetals.smoke;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/** Validation-only mod that proves the packaged server loads the exact release jars. */
@Mod(PackagedServerSmoke.MOD_ID)
public final class PackagedServerSmoke {
    public static final String MOD_ID = "basemetals_server_smoke";
    private static final Logger LOGGER = LogUtils.getLogger();

    public PackagedServerSmoke() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) throws IOException {
        String baseMetals = version("basemetals");
        String oreSpawn = version("orespawn");
        if (!"3.0.1.118021".equals(baseMetals) || !"4.0.16.118021".equals(oreSpawn)) {
            throw new IllegalStateException("PACKAGED_SERVER_SMOKE FAIL versions basemetals="
                    + baseMetals + " orespawn=" + oreSpawn);
        }
        Properties result = new Properties();
        result.setProperty("basemetals", baseMetals);
        result.setProperty("orespawn", oreSpawn);
        result.setProperty("forge", version("forge"));
        Path marker = event.getServer().getServerDirectory().toPath()
                .resolve("packaged-server-smoke-pass.properties");
        try (var writer = Files.newBufferedWriter(marker, StandardCharsets.UTF_8)) {
            result.store(writer, "Base Metals packaged dedicated-server smoke");
        }
        LOGGER.info("BASEMETALS_PACKAGED_SERVER_SMOKE PASS basemetals={} orespawn={} forge={}",
                baseMetals, oreSpawn, result.getProperty("forge"));
        event.getServer().halt(false);
    }

    private static String version(String modId) {
        return ModList.get().getModContainerById(modId)
                .orElseThrow(() -> new IllegalStateException("Missing required packaged mod " + modId))
                .getModInfo().getVersion().toString();
    }
}
