package zone.moddev.mc.basemetals.content;

import java.util.List;

import net.minecraft.block.BlockPressurePlate;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** A pressure plate which responds to players only. */
public final class HumanDetectorBlock extends BlockPressurePlate {
    public HumanDetectorBlock(Properties properties) {
        super(Sensitivity.EVERYTHING, properties);
    }

    @Override
    public int computeRedstoneStrength(World world, BlockPos pos) {
        AxisAlignedBB box = PRESSURE_AABB.offset(pos);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, box);
        for (EntityPlayer player : players) {
            if (!player.isSpectator() && !player.doesEntityNotTriggerPressurePlate()) return 15;
        }
        return 0;
    }
}
