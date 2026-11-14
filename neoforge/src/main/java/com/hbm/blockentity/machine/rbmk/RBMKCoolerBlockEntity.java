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

package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.util.ChunkUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class RBMKCoolerBlockEntity extends RBMKBaseBlockEntity
        implements FluidTankHandler, FluidTankAccess, SyncUnitSchema {
    public final FluidTankNTM coolant = new FluidTankNTM(NTMFluids.PERFLUOROMETHYL_COLD, 4_000);
    public final FluidTankNTM product = new FluidTankNTM(NTMFluids.PERFLUOROMETHYL, 4_000);
    private final FluidTankNTM[] intake = {coolant};
    private final FluidTankNTM[] products = {product};
    private final FluidTankNTM[] tanks = {coolant, product};
    private final RBMKBaseBlockEntity[] columns = new RBMKBaseBlockEntity[25];
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private final FluidFlushOutputs delivery = new FluidFlushOutputs();
    private int discoveryDelay;

    public RBMKCoolerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_COOLER.get(), pos, state, 0);
        coolant.bindOwner(this);
        product.bindOwner(this);
    }

    @Override
    public void tickServer() {
        var server = (ServerLevel) level;
        if (discoveryDelay-- <= 0) {
            int index = 0;
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    cursor.setWithOffset(worldPosition, x, 0, z);
                    columns[index++] =
                            ChunkUtil.blockEntityIfLoaded(server, cursor)
                                            instanceof RBMKBaseBlockEntity column
                                    ? column
                                    : null;
                }
            }
            discoveryDelay = 60;
        }
        var warmFluid = NTMFluids.PERFLUOROMETHYL;
        if (coolant.getFill() >= 50
                && product.getMaxFill() - product.getFill() >= 50
                && product.accepts(warmFluid)) {
            coolant.setFill(coolant.getFill() - 50);
            product.receive(warmFluid, 50);
            setChanged();
            for (var column : columns) {
                if (column == null || column.isRemoved()) continue;
                double cooled = Math.max(column.heat - 200.0, 20.0);
                if (cooled == column.heat) continue;
                column.hbm$syncSet$RBMKBaseBlockEntity$heat(cooled);
                column.setChanged();
            }
        }
        delivery.provide(server, this);
        super.tickServer();
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return intake;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return products;
    }

    @Override
    public int tankCount() {
        return tanks.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[index];
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(product, COLUMN_OUTPUTS);
    }

    @Override
    public RBMKColumnType getConsoleType() {
        return RBMKColumnType.COOLER;
    }

    @Override
    public void writeDiagnostics(CompoundTag tag) {
        super.writeDiagnostics(tag);
        writeDiagnostics(tag, "t0", coolant);
        writeDiagnostics(tag, "t1", product);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        coolant.serialize(output.child("t0"));
        product.serialize(output.child("t1"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("t0").ifPresent(coolant::deserialize);
        input.child("t1").ifPresent(product::deserialize);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 48L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 4 -> coolant.packetSerialize(output);
            case 5 -> product.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 4 -> coolant.packetDeserialize(input);
            case 5 -> product.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(RBMKCoolerBlockEntity.class, "coolant", 3, 16);
        com.hbm.packet.SyncBindings.field(RBMKCoolerBlockEntity.class, "product", 3, 32);
    }
}
