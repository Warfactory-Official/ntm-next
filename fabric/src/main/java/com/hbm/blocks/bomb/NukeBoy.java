// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.tileentity.bomb.BlockEntityNukeBoy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class NukeBoy extends NukeAssemblyBase {

    public NukeBoy(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityNukeBoy(pos, state);
    }

    @Override
    protected void ignite(Level level, BlockPos pos, int yield, @Nullable Entity detonator) {
        playBoom(level, pos);
        EntityNukeExplosionMK5 mk5 =
                EntityNukeExplosionMK5.statFac(
                        level, yield, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (detonator != null) mk5.setDetonator(detonator);
        level.addFreshEntity(mk5);
        EntityNukeTorex.statFac(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, yield);
    }
}
