// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.turret;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.turret.BlockEntityTurretHIMARS;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class TurretHIMARS extends TurretBaseArtillery {

    public TurretHIMARS(Properties props) {
        super(props);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.TURRET_HIMARS).powerIn().itemsAtCells();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityTurretHIMARS(pos, state);
    }
}
