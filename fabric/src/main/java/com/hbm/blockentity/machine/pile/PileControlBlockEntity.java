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

package com.hbm.blockentity.machine.pile;

import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.pile.PileBlock;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.util.SyncInterpolation;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class PileControlBlockEntity extends PileDeviceBaseBlockEntity
        implements RadioControlHandler, SyncUnitSchema {
    private static final double STEP = 1D / 60D;
    private static final String[] COMMANDS = {"FUN:setrods!percent", "FUN:extendrods!percent"};

    public double travel;
    public double displayedTravel;
    public double previousTravel;
    public double requestedTravel;
    private boolean previousSignal;
    private final SyncInterpolation easing = new SyncInterpolation();
    private final BlockPos.MutableBlockPos signalContact = new BlockPos.MutableBlockPos();

    public PileControlBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_CONTROL.get(), pos, state, 0);
    }

    public void setTargetPosition(double value) {
        double bounded = Math.clamp(value, 0D, 1D);
        if (requestedTravel != bounded) {
            requestedTravel = bounded;
            setChanged();
        }
    }

    @Override
    public void tickServer() {
        var channel = attachedChannel(PileBlock.Role.CONTROL);
        if (channel != null) {
            if (channel.control != travel) {
                channel.control = travel;
                channel.coreChanged();
            }
            if (travel != requestedTravel) {
                double gap = requestedTravel - travel;
                hbm$syncSet$PileControlBlockEntity$travel(
                        Math.abs(gap) <= STEP
                                ? requestedTravel
                                : travel + Math.copySign(STEP, gap));
                setChanged();
            }
        }
        var facing = orientation();
        boolean signal =
                level.getSignal(signalContact.set(worldPosition).move(facing), facing.getOpposite())
                        > 0;
        if (signal != previousSignal) {
            setTargetPosition(signal ? 1D : 0D);
            previousSignal = signal;
            setChanged();
        }
        networkPackNT(100);
    }

    @Override
    public void tickClient() {
        previousTravel = displayedTravel;
        displayedTravel = easing.step(displayedTravel, travel);
    }

    @Override
    public String[] getFunctionInfo() {
        return COMMANDS;
    }

    @Override
    public void runRadioFunction(String key, String[] params) {
        if (params.length == 0) return;
        switch (key) {
            case "FUN:setrods" ->
                    setTargetPosition(
                            Math.clamp(RadioCommandParser.intParam(params[0]), 0, 100) / 100D);
            case "FUN:extendrods" ->
                    setTargetPosition(
                            requestedTravel
                                    + Math.clamp(RadioCommandParser.intParam(params[0]), -100, 100)
                                            / 100D);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$PileControlBlockEntity$travel(input.getDoubleOr("level", 0D));
        requestedTravel = input.getDoubleOr("targetLevel", 0D);
        previousSignal = input.getBooleanOr("redstone", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("level", travel);
        output.putDouble("targetLevel", requestedTravel);
        output.putBoolean("redstone", previousSignal);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 5L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeDouble(travel);
            case 2 -> output.writeInt(channelNumber);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> {
                double sample = input.readDouble();
                if (sample != travel) {
                    hbm$syncSet$PileControlBlockEntity$travel(sample);
                    easing.restart(2);
                }
            }
            case 2 -> hbm$syncSet$PileDeviceBaseBlockEntity$channelNumber(input.readInt());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$PileControlBlockEntity$travel(double next) {
        if (Double.doubleToRawLongBits(this.travel) != Double.doubleToRawLongBits(next)) {
            this.travel = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }
}
