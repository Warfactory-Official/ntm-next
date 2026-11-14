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

import com.hbm.blocks.ModBlockEntities;
import com.hbm.handler.radio.RadioSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class RadioTorchReceiverBlockEntity extends RadioTorchBlockEntity {

    public RadioTorchReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RTTY_REC.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rttyReceiver");
    }

    @Override
    public void tickServer() {
        if (!channel.isEmpty() || polling) {
            var received = channel.isEmpty() ? null : RadioSystem.read(level, channel);
            if (received != null && (polling || received.stamp() > lastUpdate - 1)) {
                String message = received.text();
                long receiptTime = level.getGameTime();
                if (lastUpdate != receiptTime) {
                    lastUpdate = receiptTime;
                    setChanged();
                }
                if (message.equals("selfdestruct")) {
                    destroyWithExplosion();
                    return;
                }

                int output = decode(message);
                if (polling && received.stamp() < lastUpdate - 1) {
                    output = 0;
                }
                if (output != lastState) {
                    setState(output);
                }
            } else if (polling && lastState != 0) {
                setState(0);
            }
        }
        super.tickServer();
    }

    private int decode(String message) {
        if (customMap) {
            for (int strength = mapping.length - 1; strength >= 0; strength--) {
                if (mapping[strength].equals(message)) {
                    return strength;
                }
            }
            return 0;
        }

        try {
            int value = Integer.parseInt(message);
            return Math.max(0, Math.min(15, value));
        } catch (NumberFormatException _) {
            return 0;
        }
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
