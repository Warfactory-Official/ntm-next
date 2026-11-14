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

import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class SteamPumpBlockEntity extends PumpBlockEntity {
    public final FluidTankNTM steam = new FluidTankNTM(NTMFluids.STEAM, 1_000);
    public final FluidTankNTM lps = new FluidTankNTM(NTMFluids.SPENTSTEAM, 10);

    private final FluidTankNTM[] inputs = {steam};
    private final FluidTankNTM[] outputs = {water, lps};
    private final FluidTankNTM[] tanks = {water, steam, lps};

    public SteamPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_PUMP.get(), pos, state, MachineData.PUMP_STEAM_SPEED.get());
    }

    @Override
    protected boolean hasOperatingInput() {
        return steam.getFill() >= 100 && lps.getFill() < lps.getMaxFill();
    }

    @Override
    protected void consumeOperatingInput() {
        steam.setFill(steam.getFill() - 100);
        lps.setFill(lps.getFill() + 1);
    }

    @Override
    protected int generationSpeed() {
        return MachineData.PUMP_STEAM_SPEED.get();
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return inputs;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return outputs;
    }

    @Override
    public FluidTankNTM[] settingsTanks() {
        return tanks;
    }

    @Override
    public long syncUnitMask() {
        return 31L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 3 -> steam.packetSerialize(output);
            case 4 -> lps.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 3 -> steam.packetDeserialize(input);
            case 4 -> lps.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        steam.serialize(output.child("steam"));
        lps.serialize(output.child("lps"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("steam").ifPresent(steam::deserialize);
        input.child("lps").ifPresent(lps::deserialize);
    }

    static {
        com.hbm.packet.SyncBindings.field(SteamPumpBlockEntity.class, "steam", 3, 8L);
        com.hbm.packet.SyncBindings.field(SteamPumpBlockEntity.class, "lps", 3, 16L);
    }
}
