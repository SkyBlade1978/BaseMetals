package zone.moddev.mc.basemetals.content;

import java.util.Map;

import zone.moddev.mc.basemetals.material.MaterialDefinition;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

public record MaterialContent(
        MaterialDefinition definition,
        Map<String, RegistryObject<Block>> blocks,
        Map<String, RegistryObject<Item>> items) {
}
