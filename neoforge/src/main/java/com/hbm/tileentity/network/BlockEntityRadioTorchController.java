// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.api.redstoneoverradio.IRORInfo;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.RORFunctionException;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.NtmContracts;
import com.hbm.tileentity.network.RTTYSystem.RTTYChannel;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class BlockEntityRadioTorchController extends BlockEntityRadioTorch {

    private @Nullable String previous;
    private boolean receiving;
    private boolean firstTick = true;

    public BlockEntityRadioTorchController(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RTTY_CONTROLLER.get(), pos, state, NtmContracts.ROR_INTERACTIVE);
        polling = true;
    }

    @Override
    public void tickServer() {
        boolean receivingNow = false;
        IRORInteractive interactive = attached(IRORInteractive.class);
        if (!channel.isEmpty() && interactive != null) {
            RTTYChannel received = RTTYSystem.listen(level, channel);
            if (received != null) {
                String command = String.valueOf(received.signal);
                if (received.timeStamp >= level.getGameTime() - 1L && received.timeStamp != -1L) {
                    receivingNow = true;

                    if ("selfdestruct".equals(command)) {
                        destroyWithExplosion();
                        return;
                    }
                    boolean changed = !command.equals(previous);
                    if (polling || !receiving || changed) {
                        try {
                            if (!command.isEmpty()) {
                                interactive.runRORFunction(
                                        IRORInfo.PREFIX_FUNCTION
                                                + IRORInteractive.getCommand(command),
                                        IRORInteractive.getParams(command));
                            }
                        } catch (RORFunctionException ignored) {
                        }
                    }
                    if (changed) {
                        previous = command;
                        setChanged();
                    }
                }
            }
        }
        if (!firstTick || receivingNow) {
            if (receiving != receivingNow) {
                receiving = receivingNow;
                setChanged();
            }
        }
        firstTick = false;
        super.tickServer();
    }

    @Override
    protected void loadRadio(ValueInput input) {
        polling = input.getBooleanOr("p", true);
        channel = input.getStringOr("c", "");
        previous = input.getStringOr("prev", "");
        receiving = input.getBooleanOr("r", !previous.isEmpty());
    }

    @Override
    protected void saveRadio(ValueOutput output) {
        output.putBoolean("p", polling);
        output.putString("c", channel);
        if (previous != null) output.putString("prev", previous);
        output.putBoolean("r", receiving);
    }

    public void setChannelName(String next) {
        if (next.equals(channel)) return;
        channel = next;
        previous = null;
        receiving = false;
        setChanged();
    }

    @Override
    protected void receiveRadioControl(CompoundTag data) {
        if (data.contains("p")) polling = data.getBooleanOr("p", polling);
        if (data.contains("c")) setChannelName(data.getStringOr("c", channel));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() & ~0xcL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            default -> super.readSyncUnit(unit, input);
        }
    }
}
