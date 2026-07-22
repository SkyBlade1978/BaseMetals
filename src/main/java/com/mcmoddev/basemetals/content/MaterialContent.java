package com.mcmoddev.basemetals.content;

import java.util.Map;

import com.mcmoddev.basemetals.material.MaterialDefinition;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

public record MaterialContent(
        MaterialDefinition definition,
        Map<String, RegistryObject<Block>> blocks,
        Map<String, RegistryObject<Item>> items) {
}
