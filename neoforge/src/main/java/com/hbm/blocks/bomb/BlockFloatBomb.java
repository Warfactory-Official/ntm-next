// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.explosion.ExplosionChaos;
import com.hbm.interfaces.IBomb;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class BlockFloatBomb extends Block implements IBomb {

    private static final int RADIUS = 15;
    private static final int LIFT = 50;

    public BlockFloatBomb(Properties props) {
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
        if (!level.isClientSide() && level.hasNeighborSignal(pos)) {
            explode(level, pos, null);
        }
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {

        level.playSound(
                null,
                pos,
                ModSounds.GUN_SPARK_SHOOT.get(),
                SoundSource.BLOCKS,
                5.0F,
                level.getRandom().nextFloat() * 0.2F + 0.9F);

        if (!level.isClientSide()) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            ExplosionChaos.floater(level, pos, RADIUS, LIFT);
            ExplosionChaos.move(level, pos, RADIUS, 0, LIFT, 0);
        }

        return BombReturnCode.DETONATED;
    }
}
