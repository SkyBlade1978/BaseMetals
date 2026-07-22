package com.mcmoddev.basemetals;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ModTabs {
    public static final CreativeModeTab BLOCKS = tab("blocks", new ItemStack(Items.IRON_BLOCK));
    public static final CreativeModeTab ITEMS = tab("items", new ItemStack(Items.IRON_INGOT));
    public static final CreativeModeTab TOOLS = tab("tools", new ItemStack(Items.IRON_PICKAXE));
    public static final CreativeModeTab COMBAT = tab("combat", new ItemStack(Items.IRON_SWORD));

    private ModTabs() {}

    private static CreativeModeTab tab(String suffix, ItemStack icon) {
        return new CreativeModeTab(BaseMetals.MOD_ID + "." + suffix) {
            @Override
            public ItemStack makeIcon() {
                return icon.copy();
            }
        };
    }
}
