// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectTiny;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.interfaces.IBomb;
import com.hbm.particle.helper.ExplosionCreator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class BombFlameWar extends Block implements IBomb {

    public BombFlameWar(Properties props) {
        super(props);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (level.hasNeighborSignal(pos)) explode(level, pos, null);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.DETONATED;

        level.destroyBlock(pos, false);
        for (int i = 0; i < 150; i++) {
            ExplosionVNT blast =
                    new ExplosionVNT(
                            level,
                            pos.getX() + level.getRandom().nextInt(51) - 25,
                            pos.getY() + level.getRandom().nextInt(11) - 5,
                            pos.getZ() + level.getRandom().nextInt(51) - 25,
                            4F,
                            null);
            blast.setEntityProcessor(new EntityProcessorCrossSmooth(1D, 25F));
            blast.setPlayerProcessor(new PlayerProcessorStandard());
            blast.setSFX(new ExplosionEffectTiny());
            blast.explode();
        }

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        ExplosionVNT blast = new ExplosionVNT(level, x, y, z, 15F);
        blast.setBlockAllocator(new BlockAllocatorStandard(32));
        blast.setBlockProcessor(new BlockProcessorStandard().setNoDrop());
        blast.setEntityProcessor(new EntityProcessorCrossSmooth(2D, 200F));
        blast.setPlayerProcessor(new PlayerProcessorStandard());
        blast.explode();
        ExplosionCreator.composeEffectSmall(level, x, y, z);

        return BombReturnCode.DETONATED;
    }
}
