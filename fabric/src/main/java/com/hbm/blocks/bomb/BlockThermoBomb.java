// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.explosion.ExplosionThermo;
import com.hbm.interfaces.IBomb;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public abstract class BlockThermoBomb extends Block implements IBomb {

    protected static final int FIELD_RADIUS = 15;
    protected static final int ENTITY_RADIUS = 20;
    private static final float BLAST = 5.0F;

    protected BlockThermoBomb(Properties props) {
        super(props);
    }

    protected abstract void field(Level level, BlockPos pos);

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (!level.isClientSide() && level.hasNeighborSignal(pos)) {
            explode(level, pos, null);
        }
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        field(level, pos);

        level.explode(
                null, pos.getX(), pos.getY(), pos.getZ(), BLAST, Level.ExplosionInteraction.BLOCK);
        return BombReturnCode.DETONATED;
    }

    public static class Endothermic extends BlockThermoBomb {

        public Endothermic(Properties props) {
            super(props);
        }

        @Override
        protected void field(Level level, BlockPos pos) {
            ExplosionThermo.freeze(level, pos, FIELD_RADIUS);
            ExplosionThermo.freezer(level, pos, ENTITY_RADIUS);
        }
    }

    public static class Exothermic extends BlockThermoBomb {

        public Exothermic(Properties props) {
            super(props);
        }

        @Override
        protected void field(Level level, BlockPos pos) {
            ExplosionThermo.scorch(level, pos, FIELD_RADIUS);
            ExplosionThermo.setEntitiesOnFire(level, pos, ENTITY_RADIUS);
        }
    }
}
