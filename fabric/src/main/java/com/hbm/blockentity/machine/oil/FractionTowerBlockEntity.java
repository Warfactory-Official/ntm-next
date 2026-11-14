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

package com.hbm.blockentity.machine.oil;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.FractionRecipe;
import com.hbm.inventory.recipes.FractionRecipes;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class FractionTowerBlockEntity extends MachineBlockEntity
        implements FluidTankAccess, FluidTankHandler, FluidSettingsHandler, SyncUnitSchema {

    public static final int TANK_CAPACITY = 4000;

    public final FluidTankNTM[] tanks = new FluidTankNTM[3];
    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public FractionTowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FRACTION_TOWER.get(), pos, state, 0);
        hbm$syncArraySet$FractionTowerBlockEntity$tanks(
                tanks, 0, new FluidTankNTM(NTMFluids.HEAVYOIL, TANK_CAPACITY));
        hbm$syncArraySet$FractionTowerBlockEntity$tanks(
                tanks, 1, new FluidTankNTM(NTMFluids.BITUMEN, TANK_CAPACITY));
        hbm$syncArraySet$FractionTowerBlockEntity$tanks(
                tanks, 2, new FluidTankNTM(NTMFluids.SMEAR, TANK_CAPACITY));
        receiving = new FluidTankNTM[] {tanks[0]};
        sending = new FluidTankNTM[] {tanks[1], tanks[2]};
    }

    @Override
    public void tickServer() {
        if (level.getBlockEntity(worldPosition.above(3))
                instanceof FractionTowerBlockEntity above) {
            for (int i = 0; i < 3; i++) above.tanks[i].setTankType(tanks[i].getTankType());

            int oil =
                    Math.min(
                            tanks[0].getFill(),
                            above.tanks[0].getMaxFill() - above.tanks[0].getFill());
            int left =
                    Math.min(above.tanks[1].getFill(), tanks[1].getMaxFill() - tanks[1].getFill());
            int right =
                    Math.min(above.tanks[2].getFill(), tanks[2].getMaxFill() - tanks[2].getFill());

            move(tanks[0], above.tanks[0], oil);
            move(above.tanks[1], tanks[1], left);
            move(above.tanks[2], tanks[2], right);
            if (oil > 0 || left > 0 || right > 0) {
                markChanged();
                above.markChanged();
            }
        }

        setupTanks();
        if (TickPhase.every(this, 10)) fractionate();

        flush.provide((ServerLevel) level, this);

        networkPackNT(50);
    }

    private static void move(FluidTankNTM from, FluidTankNTM to, int amount) {
        Fluid fluid = from.getFluid();
        if (to.accepts(fluid)) from.setFill(from.getFill() - to.receive(fluid, amount));
    }

    private void setupTanks() {
        FractionRecipe split = FractionRecipes.INSTANCE.getFractions(tanks[0].getTankType());
        if (split != null) {
            tanks[1].setTankType(split.outputFluid[0].type());
            tanks[2].setTankType(split.outputFluid[1].type());
        } else {
            tanks[0].setTankType(null);
            tanks[1].setTankType(null);
            tanks[2].setTankType(null);
        }
    }

    private void fractionate() {
        FractionRecipe split = FractionRecipes.INSTANCE.getFractions(tanks[0].getTankType());
        if (split == null) return;

        int drain = (int) split.inputFluid[0].amount();
        int left = (int) split.outputFluid[0].amount();
        int right = (int) split.outputFluid[1].amount();

        if (tanks[0].getFill() >= drain && hasSpace(left, right)) {
            tanks[0].setFill(tanks[0].getFill() - drain);
            tanks[1].setFill(tanks[1].getFill() + left);
            tanks[2].setFill(tanks[2].getFill() + right);
            markChanged();
        }
    }

    private boolean hasSpace(int left, int right) {
        return tanks[1].getFill() + left <= tanks[1].getMaxFill()
                && tanks[2].getFill() + right <= tanks[2].getMaxFill();
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
        for (FluidTankNTM tank : getSendingTanks()) out.add(tank, FlushFaces.own());
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < 3; i++) {
            FluidTankNTM tank = tanks[i];
            input.child("tank" + i).ifPresent(tank::deserialize);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < 3; i++) tanks[i].serialize(output.child("tank" + i));
    }

    private void writeTanks(ByteBuf output) {
        for (int i = 0; i < 3; i++) tanks[i].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        for (int i = 0; i < 3; i++) tanks[i].packetDeserialize(input);
    }

    @Override
    public long syncUnitMask() {
        return 1L << 0;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeTanks(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readTanks(input);
            default -> throw new IllegalArgumentException();
        }
    }

    public final FluidTankNTM hbm$syncArraySet$FractionTowerBlockEntity$tanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.tanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.tanks) {
                syncArrayChanged(array, index, 3, 1L << 0);
            }
        }
        return next;
    }

    static {
        com.hbm.packet.SyncBindings.field(FractionTowerBlockEntity.class, "tanks", 3, 1L << 0);
    }
}
