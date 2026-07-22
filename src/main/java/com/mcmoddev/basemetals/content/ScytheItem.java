package com.mcmoddev.basemetals.content;

import com.mcmoddev.basemetals.material.MaterialDefinition;

import net.minecraft.world.item.SwordItem;

public final class ScytheItem extends SwordItem implements MaterialBacked {
    private final MaterialDefinition material;
    public ScytheItem(MaterialDefinition material, Properties properties) {
        super(new MaterialTier(material), 2, -2.8F, properties);
        this.material = material;
    }
    @Override public MaterialDefinition baseMetalsMaterial() { return material; }
}
