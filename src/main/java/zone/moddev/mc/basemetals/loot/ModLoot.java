package zone.moddev.mc.basemetals.loot;

import zone.moddev.mc.basemetals.BaseMetals;

import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModLoot {
    public static final DeferredRegister<GlobalLootModifierSerializer<?>> SERIALIZERS = DeferredRegister.create(
            ForgeRegistries.Keys.LOOT_MODIFIER_SERIALIZERS, BaseMetals.MOD_ID);
    public static final RegistryObject<GlobalLootModifierSerializer<AppendLootTableModifier>> ADD_TABLE =
            SERIALIZERS.register("add_table", AppendLootTableModifier.Serializer::new);

    private ModLoot() {}
    public static void register(IEventBus bus) { SERIALIZERS.register(bus); }
}
