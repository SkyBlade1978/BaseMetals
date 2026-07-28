package zone.moddev.mc.basemetals;

import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModTabs {
    public static final CreativeModeTab BLOCKS = tab("blocks", "starsteel_block", Items.IRON_BLOCK);
    public static final CreativeModeTab ITEMS = tab("items", "starsteel_gear", Items.IRON_INGOT);
    public static final CreativeModeTab TOOLS = tab("tools", "starsteel_pickaxe", Items.IRON_PICKAXE);
    public static final CreativeModeTab COMBAT = tab("combat", "starsteel_sword", Items.IRON_SWORD);

    private ModTabs() {}

    private static CreativeModeTab tab(String suffix, String iconId, Item fallback) {
        Supplier<ItemStack> icon = () -> {
            Item registered = ForgeRegistries.ITEMS.getValue(new ResourceLocation(BaseMetals.MOD_ID, iconId));
            return new ItemStack(registered == null ? fallback : registered);
        };
        return new CreativeModeTab(BaseMetals.MOD_ID + "." + suffix) {
            @Override
            public ItemStack makeIcon() {
                return icon.get();
            }
        };
    }
}
