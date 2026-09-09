package zone.moddev.mc.basemetals.testmod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import zone.moddev.mc.basemetals.ModTabs;
import zone.moddev.mc.basemetals.content.FluidContent;
import zone.moddev.mc.basemetals.content.ModContent;
import zone.moddev.mc.basemetals.material.MaterialCatalogue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.registries.ForgeRegistries;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Build-only packaged-client probe; this class is never part of the public JAR. */
@Mod(BaseMetalsClientProbe.MODID)
@Mod.EventBusSubscriber(modid = BaseMetalsClientProbe.MODID, value = Dist.CLIENT)
public final class BaseMetalsClientProbe {
    static final String MODID = "basemetalsclientprobe";
    private static final String WORLD_DIRECTORY = "basemetals-client-smoke-world";
    private static final Logger LOGGER = LogManager.getLogger(MODID);
    private static volatile BaseMetalsClientProbe instance;

    private int state;
    private int stateTicks;
    private int renderedFrames;

    public BaseMetalsClientProbe() {
        instance = this;
    }

    @SubscribeEvent
    public static void onWorldRendered(RenderWorldLastEvent event) {
        BaseMetalsClientProbe probe = instance;
        if (probe != null && Boolean.getBoolean("basemetalsclientprobe.enabled") && probe.state == 1) {
            probe.renderedFrames++;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        BaseMetalsClientProbe probe = instance;
        if (probe == null || event.phase != TickEvent.Phase.END
                || !Boolean.getBoolean("basemetalsclientprobe.enabled")) return;
        probe.tick();
    }

    private void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (++stateTicks > 3600) fail(minecraft, "Timed out in client probe state " + state);
        try {
            if (state == 0 && minecraft.currentScreen instanceof GuiMainMenu) {
                validateClientContent(minecraft);
                minecraft.launchIntegratedServer(WORLD_DIRECTORY, "Base Metals Client Smoke",
                        new WorldSettings(0L, GameType.CREATIVE, false, false, WorldType.DEFAULT));
                nextState(1);
            } else if (state == 1 && minecraft.world != null && minecraft.player != null
                    && renderedFrames >= 8 && stateTicks >= 100) {
                writeMarker();
                LOGGER.info("BASEMETALS_CLIENT_PROBE PASS frames={}", Integer.valueOf(renderedFrames));
                minecraft.shutdown();
                nextState(2);
            }
        } catch (RuntimeException | IOException failure) {
            fail(minecraft, failure.toString());
        }
    }

    private static void validateClientContent(Minecraft minecraft) throws IOException {
        require(ModList.get().isLoaded("basemetals"), "Base Metals is not loaded");
        require(ModList.get().isLoaded("orespawn"), "OreSpawn is not loaded");
        require(MaterialCatalogue.ALL.size() == 22, "material catalogue size");
        require(ModContent.blocksById().size() == 360, "block catalogue size");
        require(ModContent.itemsById().size() == 1115, "item catalogue size");
        require(ModContent.fluids().size() == 36, "fluid catalogue size");
        require(ForgeRegistries.ITEMS.containsKey(new ResourceLocation("basemetals", "mercury_bucket")),
                "mercury bucket registration");
        for (FluidContent fluid : ModContent.fluids().values()) {
            require(fluid.bucket().get().getGroup() == ModTabs.ITEMS, "bucket creative group");
        }
        validateBucketModel(minecraft, "mercury_bucket");
        validateBucketModel(minecraft, "tin_bucket");
        minecraft.getResourceManager().getResource(
                new ResourceLocation("basemetals", "textures/item/adamantine_sword.png"));
        minecraft.getResourceManager().getResource(
                new ResourceLocation("basemetals", "textures/block/adamantine_block.png"));
    }

    private static void validateBucketModel(Minecraft minecraft, String name) {
        ItemStack stack = new ItemStack(ModContent.item(name).get());
        IBakedModel model = minecraft.getItemRenderer().getItemModelWithOverrides(stack, null, null);
        Random random = new Random(42L);
        List<BakedQuad> quads = new ArrayList<BakedQuad>(model.getQuads(null, null, random));
        for (net.minecraft.util.EnumFacing side : net.minecraft.util.EnumFacing.values()) {
            quads.addAll(model.getQuads(null, side, random));
        }
        require(!quads.isEmpty(), name + " has no rendered quads");
        boolean hasTintedFluidLayer = false;
        for (BakedQuad quad : quads) {
            require(!"missingno".equals(quad.getSprite().getName().getPath()),
                    name + " uses the missing-texture sprite");
            if (quad.hasTintIndex() && quad.getTintIndex() == 1) hasTintedFluidLayer = true;
        }
        require(hasTintedFluidLayer, name + " has no tintable fluid layer");
        String fluidName = name.substring(0, name.length() - "_bucket".length());
        FluidContent fluid = ModContent.fluids().get(fluidName);
        require(fluid != null, name + " has no matching fluid content");
        require(minecraft.getItemColors().getColor(stack, 1) == ModContent.fluidColour(fluid.source().get()),
                name + " does not use its material fluid colour");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private void writeMarker() throws IOException {
        Properties values = new Properties();
        values.setProperty("content_verified", "true");
        values.setProperty("integrated_world_rendered", Boolean.toString(renderedFrames >= 8));
        values.setProperty("rendered_frames", Integer.toString(renderedFrames));
        try (FileOutputStream output = new FileOutputStream(new File("client-smoke-pass.properties"))) {
            values.store(output, "Base Metals Forge 1.13.2 packaged-client gate");
        }
    }

    private void nextState(int next) {
        state = next;
        stateTicks = 0;
    }

    private static void fail(Minecraft minecraft, String message) {
        try {
            Properties values = new Properties();
            values.setProperty("failure", message);
            try (FileOutputStream output = new FileOutputStream(new File("client-smoke-failure.properties"))) {
                values.store(output, "Base Metals packaged-client failure");
            }
        } catch (IOException ignored) {
        }
        minecraft.shutdown();
        throw new IllegalStateException(message);
    }
}
