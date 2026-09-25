// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network.pneumatic;

import com.hbm.api.ntl.PneumaticNetwork;
import com.hbm.inventory.IGUIProvider;
import com.hbm.tileentity.BlockEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public abstract class BlockEntityPneumaticMachineBase extends BlockEntityMachineBase
        implements IGUIProvider {

    protected BlockEntityPneumaticMachineBase(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    public @Nullable PneumaticNetwork net() {
        return level instanceof ServerLevel sl ? PneumaticNetwork.at(sl, worldPosition) : null;
    }
}
