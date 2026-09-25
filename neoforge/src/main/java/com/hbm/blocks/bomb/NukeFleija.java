// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.tileentity.bomb.BlockEntityNukeFleija;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class NukeFleija extends NukeAssemblyBase {

    public NukeFleija(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityNukeFleija(pos, state);
    }

    @Override
    protected void ignite(Level level, BlockPos pos, int yield, @Nullable Entity detonator) {
        EntityNukeExplosionMK3 blast =
                EntityNukeExplosionMK3.statFacFleija(
                        level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, yield);
        if (blast.isRemoved()) return;

        playBoom(level, pos);
        level.addFreshEntity(blast);
        level.addFreshEntity(
                EntityCloudFleija.statFac(level, yield, pos.getX(), pos.getY(), pos.getZ()));
    }
}
