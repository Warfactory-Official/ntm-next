// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.bomb;

import com.hbm.tileentity.BlockEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BlockEntityNukeAssembly extends BlockEntityMachineBase
        implements MenuProvider {

    protected BlockEntityNukeAssembly(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    @Override
    protected double interactionRangeSq() {
        return 64;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    public abstract boolean isReady();

    public abstract int yield();

    public void clearSlots() {
        clearContent();
    }
}
