// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.api.block.IFuckingExplode;
import com.hbm.entity.item.EntityTntNtm;
import com.hbm.interfaces.IBomb;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public abstract class BlockExplosiveCharge extends Block
        implements IBomb, IFuckingExplode, IDetConnectible {

    protected BlockExplosiveCharge(Properties props) {
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
    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
        ChainDetonation.spawn(
                level, pos, explosion.getIndirectSourceEntity(), defaultBlockState(), 0);
    }

    @Override
    public void explodeEntity(Level level, double x, double y, double z, EntityTntNtm entity) {
        explode(level, BlockPos.containing(x, y, z), entity);
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }
}
