package zone.moddev.mc.basemetals.content;

import zone.moddev.mc.basemetals.config.BaseMetalsConfig;

import net.minecraft.block.BlockFlowingFluid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class MoltenMetalBlock extends BlockFlowingFluid {
    private final boolean mercury;

    public MoltenMetalBlock(FlowingFluid fluid, Properties properties, boolean mercury) {
        super(fluid, properties);
        this.mercury = mercury;
    }

    @Override
    public void onEntityCollision(IBlockState state, World world, BlockPos pos, Entity entity) {
        super.onEntityCollision(state, world, pos, entity);
        if (mercury && !world.isRemote && BaseMetalsConfig.MERCURY_EFFECTS.get()
                && entity instanceof EntityLivingBase && world.rand.nextInt(32) == 0) {
            ((EntityLivingBase) entity).addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 30 * 20, 2));
        }
    }
}
