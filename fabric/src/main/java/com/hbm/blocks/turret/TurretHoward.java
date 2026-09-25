// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.turret;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.turret.BlockEntityTurretHoward;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class TurretHoward extends TurretBaseNT {

    public TurretHoward(Properties props) {
        super(props);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.TURRET_HOWARD).powerIn().itemsAtCells();
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        passiveEverywhere(core, facing, visitor, PASSIVE_POWER_IN | PASSIVE_ITEMS);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityTurretHoward(pos, state);
    }
}
