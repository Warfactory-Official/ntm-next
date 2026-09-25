// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.tileentity.network.RTTYSystem.RTTYChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class BlockEntityRadioTorchReceiver extends BlockEntityRadioTorch {

    public BlockEntityRadioTorchReceiver(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RTTY_REC.get(), pos, state);
    }

    @Override
    public void tickServer() {
        if (!channel.isEmpty() || polling) {
            RTTYChannel received = channel.isEmpty() ? null : RTTYSystem.listen(level, channel);
            if (received != null
                    && (polling
                            || received.timeStamp > lastUpdate - 1L && received.timeStamp != -1L)) {
                String message = String.valueOf(received.signal);
                lastUpdate = level.getGameTime();
                if ("selfdestruct".equals(message)) {
                    destroyWithExplosion();
                    return;
                }

                int nextState = 0;
                if (customMap) {
                    for (int i = 15; i >= 0; i--) {
                        if (message.equals(mapping[i])) {
                            nextState = i;
                            break;
                        }
                    }
                } else {
                    try {
                        nextState = Mth.clamp(Integer.parseInt(message), 0, 15);
                    } catch (NumberFormatException ignored) {
                    }
                }

                if (polling && received.timeStamp < lastUpdate - 1L) nextState = 0;
                setState(nextState);
            } else if (polling && lastState != 0) {
                setState(0);
            }
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
