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
import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ElectricPumpBlockEntity extends PumpBlockEntity implements EnergyHandler {
    private long power;

    private final FluidTankNTM[] tanks = {water};

    public ElectricPumpBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.ELECTRIC_PUMP.get(),
                pos,
                state,
                MachineData.PUMP_ELECTRIC_SPEED.get());
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$ElectricPumpBlockEntity$power(stored);
    }

    @Override
    public long getMaxPower() {
        return 10_000L;
    }

    @Override
    protected boolean hasOperatingInput() {
        return power >= 1_000L;
    }

    @Override
    protected void consumeOperatingInput() {
        hbm$syncSet$ElectricPumpBlockEntity$power((long) (power - (1_000L)));
    }

    @Override
    protected int generationSpeed() {
        return MachineData.PUMP_ELECTRIC_SPEED.get();
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return tanks;
    }

    @Override
    public FluidTankNTM[] settingsTanks() {
        return tanks;
    }

    @Override
    public long syncUnitMask() {
        return 15L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit == 3) output.writeLong(power);
        else super.writeSyncUnit(unit, output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit == 3) hbm$syncSet$ElectricPumpBlockEntity$power(input.readLong());
        else super.readSyncUnit(unit, input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$ElectricPumpBlockEntity$power(input.getLongOr("power", 0L));
    }

    public final void hbm$syncSet$ElectricPumpBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((8L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 8L);
        }
    }
}
