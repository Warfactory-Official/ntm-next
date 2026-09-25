// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.entity.projectile.EntityFallingNuke;
import com.hbm.explosion.ExplosionNukeCustom;
import com.hbm.tileentity.bomb.BlockEntityNukeCustom;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class NukeCustom extends BombRotatableBlock {

    public NukeCustom(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityNukeCustom(pos, state);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (!(level instanceof ServerLevel server)) return BombReturnCode.UNDEFINED;
        if (!(server.getBlockEntity(pos) instanceof BlockEntityNukeCustom bomb)) {
            return BombReturnCode.UNDEFINED;
        }

        BlockEntityNukeCustom.Totals t = bomb.totals();
        if (bomb.isFalling()) {
            EntityFallingNuke falling =
                    new EntityFallingNuke(
                            server,
                            t.tnt(),
                            t.nuke(),
                            t.hydro(),
                            t.amat(),
                            t.dirty(),
                            t.schrab(),
                            t.euph());
            falling.setNukeMeta(server.getBlockState(pos).getValue(FACING));
            falling.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0F, 0F);
            bomb.clearSlots();
            server.removeBlock(pos, false);
            server.addFreshEntity(falling);
            return BombReturnCode.TRIGGERED;
        }

        bomb.clearSlots();
        server.destroyBlock(pos, false);
        ExplosionNukeCustom.explodeCustom(
                server,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                t.tnt(),
                t.nuke(),
                t.hydro(),
                t.amat(),
                t.dirty(),
                t.schrab(),
                t.euph());
        return BombReturnCode.DETONATED;
    }
}
