package com.mcmoddev.basemetals.content;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.RegistryObject;

public record FluidContent(
        RegistryObject<ForgeFlowingFluid.Source> source,
        RegistryObject<ForgeFlowingFluid.Flowing> flowing,
        RegistryObject<LiquidBlock> block,
        RegistryObject<Item> bucket) {
}
