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

import com.hbm.api.environment.Environment;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SolarBoilerBlockEntity extends MachineBlockEntity
        implements FluidTankAccess, FluidTankHandler, FluidSettingsHandler, SyncUnitSchema {
    public final FluidTankNTM water = new FluidTankNTM(NTMFluids.WATER, 100);
    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;
    public final FluidTankNTM steam = new FluidTankNTM(NTMFluids.STEAM, 10_000);
    public final Set<BlockPos> primary = new HashSet<>();
    public final Set<BlockPos> secondary = new HashSet<>();
    public int display;
    public int heat;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public SolarBoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLARBOILER.get(), pos, state, 0);
        receiving = new FluidTankNTM[] {water};
        sending = new FluidTankNTM[] {steam};
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm.machine_solar_boiler");
    }

    @Override
    public void tickServer() {
        int process = Environment.sunHeat((ServerLevel) level, worldPosition, heat) / 50;
        this.hbm$syncSet$SolarBoilerBlockEntity$display(process);
        process = Math.min(process, water.getFill());
        process = Math.min(process, (steam.getMaxFill() - steam.getFill()) / 100);

        if (process < 0) process = 0;
        if (process > 0) markChanged();

        water.setFill(water.getFill() - process);
        steam.setFill(steam.getFill() + process * 100);

        heat = 0;

        flush.provide((ServerLevel) level, this);

        networkPackNTTracking();
    }

    @Override
    public void tickClient() {
        secondary.clear();
        secondary.addAll(primary);
        primary.clear();
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(
                steam,
                (server, pos, contacts) -> {
                    contacts.contact(pos.above(3), Direction.DOWN);
                    contacts.contact(pos.below(), Direction.UP);
                });
    }

    @Override
    public int tankCount() {
        return 2;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return switch (index) {
            case 0 -> water;
            case 1 -> steam;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("water").ifPresent(water::deserialize);
        input.child("steam").ifPresent(steam::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        water.serialize(output.child("water"));
        steam.serialize(output.child("steam"));
    }

    @Override
    public long syncUnitMask() {
        return 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.display);
            case 1 -> this.water.packetSerialize(output);
            case 2 -> this.steam.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.hbm$syncSet$SolarBoilerBlockEntity$display(input.readInt());
            case 1 -> this.water.packetDeserialize(input);
            case 2 -> this.steam.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$SolarBoilerBlockEntity$display(int next) {
        if (this.display != next) {
            this.display = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(SolarBoilerBlockEntity.class, "water", 3, 1L << 1);
        com.hbm.packet.SyncBindings.field(SolarBoilerBlockEntity.class, "steam", 3, 1L << 2);
    }
}
