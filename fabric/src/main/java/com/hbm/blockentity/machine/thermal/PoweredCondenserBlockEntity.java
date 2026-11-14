/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

package com.hbm.blockentity.machine.thermal;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.data.MachineData;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class PoweredCondenserBlockEntity extends SteamCondenserBlockEntity
        implements EnergyHandler {
    public long power;
    public float spin;
    public float prevSpin;

    public PoweredCondenserBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.CONDENSER_POWERED.get(),
                pos,
                state,
                MachineData.CONDENSER_POWERED_INPUT_TANK_SIZE.get(),
                MachineData.CONDENSER_POWERED_OUTPUT_TANK_SIZE.get());
    }

    @Override
    protected boolean extraCondition(int amount) {
        return power >= 0.95D * amount * MachineData.CONDENSER_POWERED_POWER_CONSUMPTION.get();
    }

    @Override
    protected void postConvert(int amount) {
        hbm$syncSet$PoweredCondenserBlockEntity$power(
                Math.max(
                        0L,
                        power
                                - (long) amount
                                        * MachineData.CONDENSER_POWERED_POWER_CONSUMPTION.get()));
    }

    @Override
    protected boolean vacuumProof() {
        return true;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$PoweredCondenserBlockEntity$power(stored);
    }

    @Override
    public long getMaxPower() {
        return MachineData.CONDENSER_POWERED_MAX_POWER.get();
    }

    @Override
    public void tickClient() {
        prevSpin = spin;
        if (waterTimer <= 0) return;
        spin += 30F;
        if (spin >= 360F) {
            spin -= 360F;
            prevSpin -= 360F;
        }
        if (TickPhase.every(this, 4)) {
            var facing = MultiblockCoreBlock.coreFacing(getBlockState());
            double dx = facing.getStepX();
            double dz = facing.getStepZ();
            double x = worldPosition.getX() + 0.5;
            double y = worldPosition.getY() + 1.5;
            double z = worldPosition.getZ() + 0.5;
            level.addParticle(
                    ParticleTypes.CLOUD, x + 1.5 * dx, y, z + 1.5 * dz, 0.1 * dx, 0, 0.1 * dz);
            level.addParticle(
                    ParticleTypes.CLOUD, x - 1.5 * dx, y, z - 1.5 * dz, -0.1 * dx, 0, -0.1 * dz);
        }
    }

    @Override
    protected void loadAdditional(ValueInput data) {
        super.loadAdditional(data);
        hbm$syncSet$PoweredCondenserBlockEntity$power(data.getLongOr("power", power));
    }

    @Override
    protected void saveAdditional(ValueOutput data) {
        super.saveAdditional(data);
        data.putLong("power", power);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << 3;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf bytes) {
        if (unit == 3) bytes.writeLong(power);
        else super.writeSyncUnit(unit, bytes);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf bytes) {
        if (unit == 3) hbm$syncSet$PoweredCondenserBlockEntity$power(bytes.readLong());
        else super.readSyncUnit(unit, bytes);
    }

    public final void hbm$syncSet$PoweredCondenserBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }
}
