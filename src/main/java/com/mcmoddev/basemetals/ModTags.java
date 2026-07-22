package com.mcmoddev.basemetals;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    public static final TagKey<Block> SCYTHE_HARVESTABLE = BlockTags.create(
            new ResourceLocation(BaseMetals.MOD_ID, "scythe_harvestable"));

    private ModTags() {}
}
