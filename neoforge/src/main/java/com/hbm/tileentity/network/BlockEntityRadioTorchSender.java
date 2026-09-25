// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class BlockEntityRadioTorchSender extends BlockEntityRadioTorch {

    public BlockEntityRadioTorchSender(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RTTY_SENDER.get(), pos, state);
    }

    @Override
    public void tickServer() {
        Direction direction = supportDirection();
        BlockPos support = worldPosition.relative(direction);
        int input = level.getSignal(support, direction.getOpposite());
        BlockState supportState = level.getBlockState(support);
        if (supportState.hasAnalogOutputSignal()) {
            input = supportState.getAnalogOutputSignal(level, support, direction.getOpposite());
        }

        boolean shouldSend = polling;
        if (input != lastState) {
            setState(input, false);
            shouldSend = true;
        }

        if (shouldSend && !channel.isEmpty()) {
            String signal = customMap ? mapping[input] : Integer.toString(input);
            if (!signal.isEmpty()) RTTYSystem.broadcast(level, channel, signal);
        }
        super.tickServer();
    }

    @Override
    protected void loadRadio(ValueInput input) {
        loadMapped(input);
    }

    @Override
    protected void saveRadio(ValueOutput output) {
        saveMapped(output);
    }

    @Override
    protected void receiveRadioControl(CompoundTag data) {
        receiveMapped(data);
    }
}
