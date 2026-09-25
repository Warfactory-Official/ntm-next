// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.rbmk;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBase;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKOutgasser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RBMKOutgasser extends RBMKBase implements ICapabilityBlock {

    public RBMKOutgasser(Properties properties) {
        super(properties);
    }

    @Override
    public int coreMask() {
        return MASK_DOWN;
    }

    @Override
    protected int topMask() {
        return MASK_UP;
    }

    @Override
    protected BlockEntityType<? extends BlockEntityRBMKBase> beType() {
        return ModBlockEntities.RBMK_OUTGASSER.get();
    }

    @Override
    protected BlockEntityRBMKBase createCore(BlockPos pos, BlockState state) {
        return new BlockEntityRBMKOutgasser(pos, state);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        visitCells(
                core,
                facing,
                (pos, mask) -> {
                    if (mask == MASK_UP) visitor.passiveCell(pos, MASK_ALL, PASSIVE_ITEMS);
                });
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.RBMK_OUTGASSER).fluidOut().items().itemsAtCells();
    }
}
