/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blockentity.network;

import com.hbm.blockentity.ChunkPublication;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.handler.radio.RadioSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class RadioTorchSenderBlockEntity extends RadioTorchBlockEntity {

    private static final String[] DECIMAL_STRENGTHS = {
        "0", "1", "2", "3", "4", "5", "6", "7",
        "8", "9", "10", "11", "12", "13", "14", "15"
    };

    private BlockPos supportPosition;
    private BlockState supportState;
    private int directStrength;

    public RadioTorchSenderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RTTY_SENDER.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rttySender");
    }

    @Override
    public void refreshAttached() {
        super.refreshAttached();
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        var direction = supportDirection();
        var signals = ChunkPublication.signals(server, worldPosition);
        supportPosition = worldPosition.relative(direction);
        supportState = signals.getBlockState(supportPosition);
        directStrength = signals.getSignal(supportPosition, direction);
    }

    @Override
    public void tickServer() {
        int strength = readInputStrength();
        boolean shouldSend = polling;
        if (strength != lastState) {
            setState(strength, false);
            shouldSend = true;
        }

        if (shouldSend && !channel.isEmpty()) {
            String message = DECIMAL_STRENGTHS[strength];
            if (customMap) {
                message = mapping[strength];
            }
            if (!message.isEmpty()) {
                RadioSystem.send(level, channel, message);
            }
        }
        super.tickServer();
    }

    private int readInputStrength() {
        if (!supportState.hasAnalogOutputSignal()) {
            return directStrength;
        }

        var server = (ServerLevel) level;
        var supportChunk =
                MultiblockCoreBlock.readableChunk(
                        server, supportPosition.getX(), supportPosition.getZ());
        if (supportChunk == null) {
            return 0;
        }
        return supportState.getAnalogOutputSignal(
                server, supportPosition, supportDirection().getOpposite());
    }

    @Override
    protected void loadRadio(ValueInput input) {
        loadMapped(input);
    }

    @Override
    protected void saveRadio(ValueOutput output) {
        saveMapped(output);
    }
}
