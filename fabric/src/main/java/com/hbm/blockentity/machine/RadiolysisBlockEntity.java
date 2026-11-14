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
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.RadiolysisMenu;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.tank.TankConversion;
import com.hbm.inventory.recipes.RadiolysisRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.RTGPelletDepletedItem;
import com.hbm.items.machine.RTGPelletItem;
import com.hbm.items.machine.RtgFuelHandler;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.util.ContagionUtil;
import com.hbm.util.InventoryUtil;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class RadiolysisBlockEntity extends MachineBlockEntity
        implements EnergyHandler,
                FluidTankHandler,
                FluidSettingsHandler,
                MenuProvider,
                SyncUnitSchema {
    public static final int INVENTORY_SIZE = 15;
    public static final int IDENTIFIER_INPUT = 10;
    public static final int IDENTIFIER_OUTPUT = 11;
    public static final int TREATMENT_INPUT = 12;
    public static final int TREATMENT_OUTPUT = 13;
    public static final int BATTERY_SLOT = 14;
    public static final int TANK_VOLUME = 2_000;
    public static final long ENERGY_CAPACITY = 1_000_000L;
    public static final int ENERGY_PER_HEAT = 10;
    public static final int CONVERSION_HEAT = 100;
    public static final int TREATMENT_HEAT = 200;
    public static final int TREATMENT_PERIOD = 100;

    private static final int[] PELLET_POSITIONS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    private static final int[] AUTOMATED_POSITIONS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 12, 13};

    private long energy;
    public int heat;
    private final FluidTankNTM[] reservoirs = {
        new FluidTankNTM(TANK_VOLUME), new FluidTankNTM(TANK_VOLUME), new FluidTankNTM(TANK_VOLUME)
    };
    private final FluidTankNTM[] intake = {reservoirs[0]};
    private final FluidTankNTM[] products = {reservoirs[1], reservoirs[2]};
    private final FluidFlushOutputs delivery = new FluidFlushOutputs();

    public RadiolysisBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIOLYSIS.get(), pos, state, INVENTORY_SIZE);
        for (var tank : reservoirs) tank.bindOwner(this);
    }

    public static int conversionPeriod(int heat) {
        return (int) Math.max(30.0 - 0.1 * (heat - CONVERSION_HEAT), 5.0);
    }

    @Override
    public void tickServer() {
        long charged = ItemEnergyTransfer.insert(this, BATTERY_SLOT, energy, false);
        storeEnergy(energy - charged);
        int currentHeat = RtgFuelHandler.current().tickPellets(this, PELLET_POSITIONS);
        if (heat != currentHeat) {
            hbm$syncSet$RadiolysisBlockEntity$heat(currentHeat);
            setChanged();
        }
        storeEnergy(Math.min(ENERGY_CAPACITY, energy + (long) heat * ENERGY_PER_HEAT));

        if (reservoirs[0].setType(IDENTIFIER_INPUT, IDENTIFIER_OUTPUT, inventory)) setChanged();
        var recipe = RadiolysisRecipes.INSTANCE.getRadiolysis(reservoirs[0].getTankType());
        if (recipe == null) {
            for (var tank : reservoirs) selectKind(tank, null);
        } else {
            var first = recipe.outputFluid[0];
            var second = recipe.outputFluid[1];
            selectKind(reservoirs[1], first.type());
            selectKind(reservoirs[2], second.type());
            if (heat > CONVERSION_HEAT && TickPhase.every(this, conversionPeriod(heat))) {
                int operations =
                        TankConversion.run(
                                reservoirs[0],
                                RadiolysisRecipes.FILL_PER_OP,
                                1,
                                reservoirs[1],
                                (int) first.amount(),
                                reservoirs[2],
                                (int) second.amount());
                if (operations != 0) setChanged();
            }
        }
        if (heat >= TREATMENT_HEAT && TickPhase.every(this, TREATMENT_PERIOD)) treatItem();
        delivery.provide((ServerLevel) level, this);
        networkPackNT(50);
    }

    private void selectKind(FluidTankNTM tank, @Nullable Fluid kind) {
        var previous = tank.getTankType();
        tank.setTankType(kind);
        if (previous != tank.getTankType()) setChanged();
    }

    private void treatItem() {
        var input = inventory.get(TREATMENT_INPUT);
        if (input.isEmpty()) return;
        if (input.has(DataComponents.FOOD) && !input.is(ModItems.PANCAKE.get()))
            removeItem(TREATMENT_INPUT, 1);
        input = inventory.get(TREATMENT_INPUT);
        if (!ContagionUtil.isContagious(input)) return;
        var cured = input.copyWithCount(1);
        ContagionUtil.cure(cured);
        if (InventoryUtil.tryAddItemToInventory(this, TREATMENT_OUTPUT, TREATMENT_OUTPUT, cured)
                .isEmpty()) {
            removeItem(TREATMENT_INPUT, 1);
        }
    }

    private void storeEnergy(long amount) {
        if (energy == amount) return;
        hbm$syncSet$RadiolysisBlockEntity$energy(amount);
        setChanged();
    }

    @Override
    public long getPower() {
        return energy;
    }

    @Override
    public long getMaxPower() {
        return ENERGY_CAPACITY;
    }

    @Override
    public void setPower(long amount) {
        storeEnergy(Math.clamp(amount, 0L, ENERGY_CAPACITY));
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
    public FluidTankNTM[] settingsTanks() {
        return reservoirs;
    }

    @Override
    public int tankCount() {
        return reservoirs.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return reservoirs[index];
    }

    @Override
    public void declareFlush(FlushLanes out) {
        var faces = FlushFaces.own();
        for (var tank : products) out.add(tank, faces);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATED_POSITIONS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == TREATMENT_INPUT
                || slot < IDENTIFIER_INPUT && stack.getItem() instanceof RTGPelletItem;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == TREATMENT_OUTPUT
                || slot < IDENTIFIER_INPUT && stack.getItem() instanceof RTGPelletDepletedItem;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.radiolysis");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RadiolysisMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", energy);
        output.putInt("heat", heat);
        reservoirs[0].serialize(output.child("input"));
        reservoirs[1].serialize(output.child("output1"));
        reservoirs[2].serialize(output.child("output2"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$RadiolysisBlockEntity$energy(input.getLongOr("power", 0L));
        hbm$syncSet$RadiolysisBlockEntity$heat(input.getIntOr("heat", 0));
        input.child("input").ifPresent(reservoirs[0]::deserialize);
        input.child("output1").ifPresent(reservoirs[1]::deserialize);
        input.child("output2").ifPresent(reservoirs[2]::deserialize);
    }

    @Override
    public long syncUnitMask() {
        return 7;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(energy);
            case 1 -> output.writeInt(heat);
            case 2 -> {
                for (var tank : reservoirs) tank.packetSerialize(output);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$RadiolysisBlockEntity$energy(input.readLong());
            case 1 -> hbm$syncSet$RadiolysisBlockEntity$heat(input.readInt());
            case 2 -> {
                for (var tank : reservoirs) tank.packetDeserialize(input);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$RadiolysisBlockEntity$energy(long next) {
        if (this.energy != next) {
            this.energy = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$RadiolysisBlockEntity$heat(int next) {
        if (this.heat != next) {
            this.heat = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    public final FluidTankNTM hbm$syncArraySet$RadiolysisBlockEntity$reservoirs(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.reservoirs && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.reservoirs) {
                syncArrayChanged(array, index, 3, 4);
            }
        }
        return next;
    }

    static {
        com.hbm.packet.SyncBindings.field(RadiolysisBlockEntity.class, "reservoirs", 3, 4);
        com.hbm.inventory.container.SyncedData.fields(RadiolysisBlockEntity.class, "heat");
    }
}
