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

import com.hbm.api.redstoneoverradio.RadioCommandException;
import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.NtmContracts;
import com.hbm.handler.radio.RadioSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class RadioTorchControllerBlockEntity extends RadioTorchBlockEntity {

    private @Nullable String previousCommand;
    private boolean receiving;
    private boolean firstTick = true;

    public RadioTorchControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RTTY_CONTROLLER.get(), pos, state, NtmContracts.ROR_INTERACTIVE);
        hbm$syncSet$RadioTorchBlockEntity$polling(true);
    }

    @Override
    public void tickServer() {
        boolean received = false;
        if (!channel.isEmpty()) {
            var target = attached(RadioControlHandler.class);
            if (target != null) {
                var frame = RadioSystem.read(level, channel);
                long gameTime = level.getGameTime();
                if (frame != null && frame.stamp() >= gameTime - 1) {
                    received = true;
                    String command = frame.text();
                    if (command.equals("selfdestruct")) {
                        destroyWithExplosion();
                        return;
                    }

                    boolean changedCommand = !command.equals(previousCommand);
                    if (!command.isEmpty() && (polling || !receiving || changedCommand)) {
                        try {
                            String function =
                                    RadioCommandParser.FUN
                                            + RadioCommandParser.commandName(command);
                            String[] parameters = RadioCommandParser.commandParams(command);
                            target.runRadioFunction(function, parameters);
                        } catch (RadioCommandException _) {
                        }
                    }
                    if (changedCommand) {
                        previousCommand = command;
                        setChanged();
                    }
                }
            }
        }

        if ((!firstTick || received) && receiving != received) {
            receiving = received;
            setChanged();
        }
        firstTick = false;
        super.tickServer();
    }

    public void setChannel(String selectedChannel) {
        if (channel.equals(selectedChannel)) return;
        hbm$syncSet$RadioTorchBlockEntity$channel(selectedChannel);
        previousCommand = null;
        receiving = false;
        setChanged();
    }

    public void send(String command) {
        if (!channel.isEmpty() && !command.isEmpty()) {
            RadioSystem.send(level, channel, command);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rttyController");
    }

    @Override
    protected void loadRadio(ValueInput input) {
        hbm$syncSet$RadioTorchBlockEntity$polling(input.getBooleanOr("p", true));
        hbm$syncSet$RadioTorchBlockEntity$channel(input.getStringOr("c", ""));
        previousCommand = input.getStringOr("prev", "");
        receiving = input.getBooleanOr("r", !previousCommand.isEmpty());
        firstTick = true;
    }

    @Override
    protected void saveRadio(ValueOutput output) {
        output.putBoolean("p", polling);
        output.putString("c", channel);
        if (previousCommand != null) output.putString("prev", previousCommand);
        output.putBoolean("r", receiving);
    }

    @Override
    public long syncUnitMask() {
        return 3L;
    }
}
