// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.rbmk;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBase;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlAuto;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RBMKControlAuto extends RBMKBase implements ICapabilityBlock {

    public RBMKControlAuto(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasOwnLid() {
        return true;
    }

    @Override
    public int coreMask() {
        return this == ModBlocks.RBMK_CONTROL_REASIM_AUTO.get() ? MASK_DOWN : MASK_NONE;
    }

    @Override
    public MachineCaps caps() {
        return this == ModBlocks.RBMK_CONTROL_REASIM_AUTO.get()
                ? MachineCaps.blockKeyed()
                        .powerIn()
                        .faces(
                                BlockEntityRBMKControlAuto.class,
                                (be, side) -> be.isPowered() && side == Direction.DOWN)
                : MachineCaps.NONE;
    }

    @Override
    protected BlockEntityType<? extends BlockEntityRBMKBase> beType() {
        return ModBlockEntities.RBMK_CONTROL_AUTO.get();
    }

    @Override
    protected BlockEntityRBMKBase createCore(BlockPos pos, BlockState state) {
        return new BlockEntityRBMKControlAuto(pos, state);
    }
}
