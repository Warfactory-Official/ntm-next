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

package com.hbm.blockentity.machine.thermal;

import com.hbm.api.environment.Environment;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SteamCondenserBlockEntity extends MachineBlockEntity
        implements ThermalFluidHandler, FluidSettingsHandler, SyncUnitSchema {
    public final FluidTankNTM input;
    public final FluidTankNTM output;
    public int waterTimer;

    private final FluidTankNTM[] tanks;
    private final FluidTankNTM[] sending;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public SteamCondenserBlockEntity(BlockPos pos, BlockState state) {
        this(
                ModBlockEntities.CONDENSER.get(),
                pos,
                state,
                MachineData.CONDENSER_INPUT_TANK_SIZE.get(),
                MachineData.CONDENSER_OUTPUT_TANK_SIZE.get());
    }

    protected SteamCondenserBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            int inputCapacity,
            int outputCapacity) {
        super(type, pos, state, 0);
        input = new FluidTankNTM(NTMFluids.SPENTSTEAM, inputCapacity);
        output = new FluidTankNTM(NTMFluids.WATER, outputCapacity);
        tanks = new FluidTankNTM[] {input, output};
        sending = new FluidTankNTM[] {output};
    }

    protected boolean extraCondition(int amount) {
        return true;
    }

    protected void postConvert(int amount) {}

    protected boolean vacuumProof() {
        return false;
    }

    @Override
    public void tickServer() {
        if (waterTimer > 0)
            hbm$syncSet$SteamCondenserBlockEntity$waterTimer((int) (waterTimer - 1));
        int amount = Math.min(input.getFill(), output.getMaxFill() - output.getFill());
        if (extraCondition(amount)) {
            input.setFill(input.getFill() - amount);
            if (amount > 0) hbm$syncSet$SteamCondenserBlockEntity$waterTimer(20);
            boolean evaporates =
                    Environment.evaporates((ServerLevel) level, worldPosition, vacuumProof());
            output.setFill(output.getFill() + (evaporates ? -amount : amount));
            postConvert(amount);
            if (amount > 0) markChanged();
        }
        flush.provide((ServerLevel) level, this);
        networkPackNT(150);
    }

    @Override
    public FluidTankNTM receivingTank() {
        return input;
    }

    @Override
    public FluidTankNTM deliveryTank() {
        return output;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public int tankCount() {
        return 2;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[index];
    }

    @Override
    public FluidTankNTM[] settingsTanks() {
        return tanks;
    }

    @Override
    protected void loadAdditional(ValueInput data) {
        super.loadAdditional(data);
        data.child("water").ifPresent(input::deserialize);
        data.child("steam").ifPresent(output::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput data) {
        super.saveAdditional(data);
        input.serialize(data.child("water"));
        output.serialize(data.child("steam"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf bytes) {
        switch (unit) {
            case 0 -> input.packetSerialize(bytes);
            case 1 -> output.packetSerialize(bytes);
            case 2 -> bytes.writeByte(waterTimer);
            default -> super.writeSyncUnit(unit, bytes);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf bytes) {
        switch (unit) {
            case 0 -> input.packetDeserialize(bytes);
            case 1 -> output.packetDeserialize(bytes);
            case 2 -> hbm$syncSet$SteamCondenserBlockEntity$waterTimer(bytes.readUnsignedByte());
            default -> super.readSyncUnit(unit, bytes);
        }
    }

    public final void hbm$syncSet$SteamCondenserBlockEntity$waterTimer(int next) {
        if (this.waterTimer != next) {
            this.waterTimer = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(SteamCondenserBlockEntity.class, "input", 3, 1L);
        com.hbm.packet.SyncBindings.field(SteamCondenserBlockEntity.class, "output", 3, 1L << 1);
    }
}
