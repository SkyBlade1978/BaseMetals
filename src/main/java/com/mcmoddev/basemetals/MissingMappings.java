package com.mcmoddev.basemetals;

import com.mcmoddev.basemetals.content.ModContent;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = BaseMetals.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MissingMappings {
    private MissingMappings() {}

    @SubscribeEvent
    public static void blocks(RegistryEvent.MissingMappings<Block> event) {
        event.getMappings(BaseMetals.MOD_ID).forEach(mapping -> {
            String path = blockTargetPath(mapping.key.getPath());
            RegistryObject<? extends Block> target = ModContent.blocksById().get(path);
            if (target != null) mapping.remap(target.get());
        });
        event.getMappings("mmdlib").forEach(mapping -> {
            RegistryObject<? extends Block> target = ModContent.blocksById()
                    .get(blockTargetPath(mapping.key.getPath()));
            if (target != null) mapping.remap(target.get());
        });
    }

    @SubscribeEvent
    public static void items(RegistryEvent.MissingMappings<Item> event) {
        event.getMappings(BaseMetals.MOD_ID).forEach(mapping -> {
            String path = itemTargetPath(mapping.key.getPath());
            RegistryObject<Item> target = ModContent.itemsById().get(path);
            if (target != null) mapping.remap(target.get());
        });
        event.getMappings("mmdlib").forEach(mapping -> {
            RegistryObject<Item> target = ModContent.itemsById()
                    .get(itemTargetPath(mapping.key.getPath()));
            if (target != null) mapping.remap(target.get());
        });
    }

    @SubscribeEvent
    public static void fluids(RegistryEvent.MissingMappings<Fluid> event) {
        event.getMappings(BaseMetals.MOD_ID).forEach(mapping -> {
            String path = fluidTargetPath(mapping.key.getPath());
            if (ModContent.fluids().containsKey(path)) mapping.remap(ModContent.fluids().get(path).source().get());
        });
        event.getMappings("mmdlib").forEach(mapping -> {
            String path = fluidTargetPath(mapping.key.getPath());
            if (ModContent.fluids().containsKey(path)) mapping.remap(ModContent.fluids().get(path).source().get());
        });
    }

    public static String blockTargetPath(String path) {
        return path.equals("liquid_mercury") ? "mercury" : path;
    }

    public static String itemTargetPath(String path) {
        return path.equals("carbon_powder") ? "coal_powder" : path;
    }

    public static String fluidTargetPath(String path) {
        return path.equals("liquid_mercury") ? "mercury" : path;
    }
}
