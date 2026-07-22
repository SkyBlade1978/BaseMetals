package com.mcmoddev.basemetals.content;

import java.util.List;
import java.util.Optional;

import com.mcmoddev.basemetals.material.MaterialDefinition;
import com.mcmoddev.basemetals.recipe.CrushingRecipe;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public final class CrackhammerItem extends PickaxeItem implements MaterialBacked {
    private final MaterialDefinition material;

    public CrackhammerItem(MaterialDefinition material, Properties properties) {
        super(new MaterialTier(material), 1, -3.0F, properties);
        this.material = material;
    }

    @Override public MaterialDefinition baseMetalsMaterial() { return material; }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide || context.getPlayer() == null) return InteractionResult.PASS;
        AABB area = new AABB(context.getClickedPos().above()).inflate(0.75D);
        List<ItemEntity> entities = context.getLevel().getEntitiesOfClass(ItemEntity.class, area,
                entity -> entity.isAlive() && !entity.getItem().isEmpty());
        boolean changed = false;
        for (ItemEntity entity : entities) {
            ItemStack input = entity.getItem();
            Optional<CrushingRecipe> recipe = context.getLevel().getRecipeManager().getRecipeFor(
                    CrushingRecipe.TYPE.get(), new SimpleContainer(input), context.getLevel());
            if (recipe.isEmpty()) continue;
            // Sneaking is the per-use full-stack control; ordinary use crushes
            // one item and never changes a persistent config contract.
            int requested = context.getPlayer().isShiftKeyDown() ? input.getCount() : 1;
            ItemStack hammer = context.getItemInHand();
            int durability = hammer.isDamageableItem()
                    ? Math.max(0, hammer.getMaxDamage() - hammer.getDamageValue())
                    : requested;
            int operations = Math.min(requested, durability);
            if (operations == 0) break;
            ItemStack output = recipe.get().getResultItem();
            input.shrink(operations);
            if (input.isEmpty()) entity.discard(); else entity.setItem(input);
            spawnOutputs(context.getLevel(), entity, output, operations);
            hammer.hurtAndBreak(operations, context.getPlayer(),
                    player -> player.broadcastBreakEvent(context.getHand()));
            changed = true;
            if (hammer.isEmpty()) break;
        }
        return changed ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    private static void spawnOutputs(Level level, ItemEntity source, ItemStack result, int operations) {
        int remaining = Math.multiplyExact(result.getCount(), operations);
        while (remaining > 0) {
            ItemStack output = result.copy();
            output.setCount(Math.min(output.getMaxStackSize(), remaining));
            remaining -= output.getCount();
            ItemEntity crushed = new ItemEntity(level, source.getX(), source.getY(), source.getZ(), output);
            crushed.setDefaultPickUpDelay();
            level.addFreshEntity(crushed);
        }
    }
}
