/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.machine;

import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.SatelliteRayEvents;
import com.hbm.world.gen.WorldgenHeight;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class SatelliteLinkerBlockEntity extends MachineBlockEntity
        implements RadioValueProvider, RadioControlHandler, SyncUnitSchema {

    private static final String[] DESCRIPTORS = {
        "VAL:connected", "VAL:freq", "VAL:rx", "VAL:type", "FUN:setfreq!freq", "FUN:tx!payload"
    };
    private static final float ANGULAR_STEP = 0.25F;
    private static final float CONNECTED_ROTATION = -15F;
    private static final float CONNECTED_ELEVATION = -45F;
    private static final float IDLE_ROTATION = 0F;
    private static final float IDLE_ELEVATION = -85F;
    private static final int MAX_READOUT_LINES = 16;

    private boolean connected;
    private int frequency;
    private List<Component> readout;

    {
        hbm$syncSet$SatelliteLinkerBlockEntity$readout(List.of());
    }

    private float rotation = IDLE_ROTATION;
    private float previousRotation = IDLE_ROTATION;
    private float elevation = IDLE_ELEVATION;
    private float previousElevation = IDLE_ELEVATION;
    private final BlockPos.MutableBlockPos columnPos = new BlockPos.MutableBlockPos();

    public SatelliteLinkerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SATLINK.get(), pos, state, 0);
    }

    public boolean connected() {
        return connected;
    }

    public int frequency() {
        return frequency;
    }

    public List<Component> readout() {
        return readout;
    }

    public void setFrequency(int value) {
        if (frequency == value) return;
        hbm$syncSet$SatelliteLinkerBlockEntity$frequency(value);
        setChanged();
    }

    public SatelliteSavedData satelliteRegistry() {
        return SatelliteSavedData.at(
                (ServerLevel) level, worldPosition.getX(), worldPosition.getZ());
    }

    @Override
    public void tickServer() {
        var server = (ServerLevel) level;
        var satellite = satelliteRegistry().getSatFromFreq(frequency);
        hbm$syncSet$SatelliteLinkerBlockEntity$connected(satellite != null && clearColumn(server));
        var lines = connected ? satellite.info(server) : List.<Component>of();
        if (lines.size() > MAX_READOUT_LINES)
            throw new IllegalArgumentException("satellite readout exceeds 16 lines");
        if (!readout.equals(lines))
            hbm$syncSet$SatelliteLinkerBlockEntity$readout(List.copyOf(lines));
        networkPackNT(150);
    }

    private boolean clearColumn(ServerLevel server) {
        var chunk =
                MultiblockCoreBlock.readableChunk(
                        server, worldPosition.getX(), worldPosition.getZ());
        columnPos.set(worldPosition);
        var top = WorldgenHeight.lightBlocking(chunk, columnPos) - 1;
        if (top > worldPosition.getY() + 6) return false;
        var packedCore = worldPosition.asLong();
        columnPos.set(worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ());
        for (var y = worldPosition.getY() + 1; y <= top; y++) {
            var state = chunk.getBlockState(columnPos.setY(y));
            if (state.getLightDampening() == 0) continue;
            if (!MultiblockSurface.isFoldedCell(state)
                    || MultiblockSurface.indexedCorePacked(
                                    server, columnPos.getX(), y, columnPos.getZ())
                            != packedCore) return false;
        }
        return true;
    }

    @Override
    public void tickClient() {
        previousRotation = rotation;
        previousElevation = elevation;
        rotation = approach(rotation, connected ? CONNECTED_ROTATION : IDLE_ROTATION);
        elevation = approach(elevation, connected ? CONNECTED_ELEVATION : IDLE_ELEVATION);
    }

    private static float approach(float current, float target) {
        return current < target
                ? Math.min(current + ANGULAR_STEP, target)
                : Math.max(current - ANGULAR_STEP, target);
    }

    public float rotation(float partialTick) {
        return previousRotation + (rotation - previousRotation) * partialTick;
    }

    public float elevation(float partialTick) {
        return previousElevation + (elevation - previousElevation) * partialTick;
    }

    @Override
    public String[] getFunctionInfo() {
        return DESCRIPTORS;
    }

    @Override
    public @Nullable String getRadioValue(String key) {
        return switch (key) {
            case "VAL:connected" -> connected ? "TRUE" : "FALSE";
            case "VAL:freq" -> Integer.toString(frequency);
            case "VAL:type", "VAL:rx" -> {
                var satellite = satelliteRegistry().getSatFromFreq(frequency);
                yield satellite == null
                        ? ""
                        : key.equals("VAL:type") ? satellite.kind().label() : satellite.reply();
            }
            default -> null;
        };
    }

    @Override
    public void runRadioFunction(String key, String[] params) {
        switch (key) {
            case "FUN:setfreq" -> {
                if (params.length == 1)
                    setFrequency(RadioCommandParser.intParam(params[0], 0, 100000));
            }
            case "FUN:tx" -> {
                var server = (ServerLevel) level;
                var registry = satelliteRegistry();
                var satellite = registry.getSatFromFreq(frequency);
                if (satellite != null) {
                    satellite.command(
                            server, String.join(RadioCommandParser.PARAM_SEP, params).split(" "));
                    registry.setDirty();
                }
                SatelliteRayEvents.report(
                        server, worldPosition, SatelliteRayEvents.RADIO_WAVES, 300);
                setChanged();
            }
            default -> {}
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$SatelliteLinkerBlockEntity$frequency(input.getIntOr("freq", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("freq", frequency);
    }

    @Override
    public long syncUnitMask() {
        return 7;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(connected);
            case 1 -> output.writeInt(frequency);
            case 2 -> {
                output.writeInt(readout.size());
                for (var line : readout)
                    ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(output, line);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$SatelliteLinkerBlockEntity$connected(input.readBoolean());
            case 1 -> hbm$syncSet$SatelliteLinkerBlockEntity$frequency(input.readInt());
            case 2 -> {
                var count = input.readInt();
                if (count < 0 || count > MAX_READOUT_LINES)
                    throw new IllegalArgumentException("invalid satellite readout count: " + count);
                var lines = new ArrayList<Component>(count);
                for (var i = 0; i < count; i++)
                    lines.add(
                            ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(input));
                hbm$syncSet$SatelliteLinkerBlockEntity$readout(List.copyOf(lines));
            }
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$SatelliteLinkerBlockEntity$connected(boolean next) {
        if (this.connected != next) {
            this.connected = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$SatelliteLinkerBlockEntity$frequency(int next) {
        if (this.frequency != next) {
            this.frequency = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    public final void hbm$syncSet$SatelliteLinkerBlockEntity$readout(List<Component> next) {
        if (this.readout != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.readout);
            this.readout = next;
            if (bound) bindSyncValue(next, 3, 4);
            if ((4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(SatelliteLinkerBlockEntity.class, "readout", 3, 4);
    }
}
