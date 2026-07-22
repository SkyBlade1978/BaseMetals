package com.mcmoddev.basemetals.client;

import com.mcmoddev.basemetals.BaseMetals;
import com.mcmoddev.basemetals.content.MaterialItems;
import com.mcmoddev.basemetals.content.ModContent;
import com.mcmoddev.basemetals.entity.ModEntities;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BaseMetals.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CUSTOM_ARROW.get(), context -> new ThrownItemRenderer<>(context, 0.75F, false));
        event.registerEntityRenderer(ModEntities.CUSTOM_BOLT.get(), context -> new ThrownItemRenderer<>(context, 0.75F, false));
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ModContent.itemsById().values().forEach(entry -> {
            net.minecraft.world.item.Item item = entry.get();
            if (item instanceof MaterialItems.Bow || item instanceof MaterialItems.Crossbow) {
                ItemProperties.register(item, new ResourceLocation("pull"), (stack, level, user, seed) ->
                        user == null || user.getUseItem() != stack ? 0.0F
                                : (float) (stack.getUseDuration() - user.getUseItemRemainingTicks()) / 20.0F);
                ItemProperties.register(item, new ResourceLocation("pulling"), (stack, level, user, seed) ->
                        user != null && user.isUsingItem() && user.getUseItem() == stack ? 1.0F : 0.0F);
            }
            if (item instanceof MaterialItems.Shield) {
                ItemProperties.register(item, new ResourceLocation("blocking"), (stack, level, user, seed) ->
                        user != null && user.isUsingItem() && user.getUseItem() == stack ? 1.0F : 0.0F);
            }
            if (item instanceof MaterialItems.FishingRod) {
                ItemProperties.register(item, new ResourceLocation("cast"), (stack, level, user, seed) -> {
                    if (user == null) return 0.0F;
                    boolean main = user.getMainHandItem() == stack;
                    boolean off = user.getOffhandItem() == stack;
                    if (user.getMainHandItem().getItem() instanceof FishingRodItem) off = false;
                    return (main || off) && user instanceof Player player && player.fishing != null ? 1.0F : 0.0F;
                });
            }
        }));
    }
}
