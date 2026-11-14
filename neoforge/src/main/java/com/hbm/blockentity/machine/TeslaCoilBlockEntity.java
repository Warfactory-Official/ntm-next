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

package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.packet.SyncList;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class TeslaCoilBlockEntity extends MachineBlockEntity
        implements EnergyHandler, SyncUnitSchema {
    public static final long MAX_POWER = 100_000L;
    public static final long POWER_PER_ZAP = 5_000L;
    public static final double OFFSET = 1.75D;

    public long power;
    public final SyncList<Vec3> arcTargets = new SyncList<>();
    private final BlockPos batteryPosition;
    private boolean meteorBattery;

    public TeslaCoilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TESLA_COIL.get(), pos, state, 0);
        batteryPosition = pos.below();
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        refreshMeteorBattery();
    }

    @Override
    public void tickServer() {
        arcTargets.clear();
        if (meteorBattery) power = MAX_POWER;
        if (power >= POWER_PER_ZAP) {
            power -= POWER_PER_ZAP;
            TeslaDischarge.append(
                    (ServerLevel) level,
                    worldPosition.getX() + .5D,
                    worldPosition.getY() + OFFSET,
                    worldPosition.getZ() + .5D,
                    10D,
                    null,
                    arcTargets);
        }
        networkPackNT(100);
    }

    public void refreshMeteorBattery() {
        boolean present = level.getBlockState(batteryPosition).is(ModBlocks.METEOR_BATTERY.get());
        if (present != meteorBattery) {
            meteorBattery = present;
            setChanged();
        }
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long stored) {
        power = stored;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        meteorBattery = input.getBooleanOr("meteorBattery", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("meteorBattery", meteorBattery);
    }

    @Override
    public long syncUnitMask() {
        return 1L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit != 0) {
            super.writeSyncUnit(unit, output);
            return;
        }
        int count = arcTargets.size();
        if (count > Short.MAX_VALUE)
            throw new IllegalArgumentException("Too many Tesla arc endpoints: " + count);
        output.writeShort(count);
        for (int i = 0; i < count; i++) {
            var endpoint = arcTargets.get(i);
            output.writeDouble(endpoint.x);
            output.writeDouble(endpoint.y);
            output.writeDouble(endpoint.z);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit != 0) {
            super.readSyncUnit(unit, input);
            return;
        }
        int count = input.readShort();
        if (count < 0)
            throw new IllegalArgumentException("Negative Tesla arc endpoint count: " + count);
        arcTargets.clear();
        for (int i = 0; i < count; i++) {
            arcTargets.add(new Vec3(input.readDouble(), input.readDouble(), input.readDouble()));
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(TeslaCoilBlockEntity.class, "arcTargets", 3, 1L);
    }
}
