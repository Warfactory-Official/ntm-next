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

package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class RBMKOutletBlockEntity extends MachineBlockEntity
        implements FluidTankHandler, FluidTankAccess, SyncUnitSchema {
    private static final Direction[] SERVICE_ORDER = {
        Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    public final FluidTankNTM reservoir = new FluidTankNTM(NTMFluids.SUPERHOTSTEAM, 32_000);
    private final FluidTankNTM[] products = {reservoir};
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private final FluidFlushOutputs delivery = new FluidFlushOutputs();

    public RBMKOutletBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_OUTLET.get(), pos, state, 0);
        reservoir.bindOwner(this);
    }

    @Override
    public void tickServer() {
        var server = (ServerLevel) level;
        var steam = NTMFluids.SUPERHOTSTEAM;
        if (RbmkParams.reasimBoilers() && reservoir.accepts(steam)) {
            for (var side : SERVICE_ORDER) {
                cursor.setWithOffset(worldPosition, side);
                var column = ColumnLookup.column(server, cursor);
                if (column == null) continue;
                int moved =
                        Math.min(reservoir.getMaxFill() - reservoir.getFill(), column.reasimSteam);
                if (moved <= 0) continue;
                reservoir.receive(steam, moved);
                column.hbm$syncSet$RBMKBaseBlockEntity$reasimSteam(
                        (int) (column.reasimSteam - (moved)));
                column.setChanged();
                setChanged();
            }
        }
        delivery.provide(server, this);
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return products;
    }

    @Override
    public int tankCount() {
        return products.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return products[index];
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(reservoir, FlushFaces.sixFaces());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        reservoir.serialize(output.child("tank"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("tank").ifPresent(reservoir::deserialize);
    }

    @Override
    public long syncUnitMask() {
        return 1;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit != 0) throw new IllegalArgumentException();
        reservoir.packetSerialize(output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit != 0) throw new IllegalArgumentException();
        reservoir.packetDeserialize(input);
    }

    static {
        com.hbm.packet.SyncBindings.field(RBMKOutletBlockEntity.class, "reservoir", 3, 1);
    }
}
