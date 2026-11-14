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
import com.hbm.blockentity.PersistentDrop;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.CatalyticReformerMenu;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.ReformingRecipes;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class CatalyticReformerBlockEntity extends MachineBlockEntity
        implements EnergyHandler,
                FluidTankHandler,
                FluidSettingsHandler,
                PersistentDrop,
                MenuProvider,
                SyncUnitSchema {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_INPUT_IN = 1;
    public static final int SLOT_INPUT_OUT = 2;
    public static final int SLOT_OUTPUT_IN_START = 3;
    public static final int SLOT_OUTPUT_OUT_START = 4;
    public static final int SLOT_FLUID_ID = 9;
    public static final int SLOT_CATALYST = 10;
    public static final int SLOT_COUNT = 11;
    public static final int OUTPUT_TANK_COUNT = 3;
    public static final int INPUT_TANK_CAPACITY = 64_000;
    public static final int OUTPUT_TANK_CAPACITY = 24_000;
    public static final long MAX_POWER = 1_000_000L;

    private static final String[] TANK_KEYS = {"input", "o1", "o2", "o3"};

    public long power;
    public final FluidTankNTM[] tanks;

    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public CatalyticReformerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CATALYTIC_REFORMER.get(), pos, state, SLOT_COUNT);
        tanks =
                new FluidTankNTM[] {
                    new FluidTankNTM(NTMFluids.NAPHTHA, INPUT_TANK_CAPACITY),
                    new FluidTankNTM(NTMFluids.REFORMATE, OUTPUT_TANK_CAPACITY),
                    new FluidTankNTM(NTMFluids.PETROLEUM, OUTPUT_TANK_CAPACITY),
                    new FluidTankNTM(NTMFluids.HYDROGEN, OUTPUT_TANK_CAPACITY)
                };
        receiving = new FluidTankNTM[] {tanks[0]};
        sending = new FluidTankNTM[] {tanks[1], tanks[2], tanks[3]};
        for (var tank : tanks) tank.bindOwner(this);
    }

    @Override
    public void tickServer() {
        setPower(power + ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false));
        if (tanks[0].setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory)) setChanged();
        if (tanks[0].loadTank(SLOT_INPUT_IN, SLOT_INPUT_OUT, inventory)) setChanged();
        processBatch();
        for (int i = 0; i < sending.length; i++) {
            if (sending[i].unloadTank(
                    SLOT_OUTPUT_IN_START + i * 2, SLOT_OUTPUT_OUT_START + i * 2, inventory)) {
                setChanged();
            }
        }
        flush.provide((ServerLevel) level, this);
        networkPackNT(150);
    }

    private void processBatch() {
        var recipe = ReformingRecipes.INSTANCE.getRecipeForInput(tanks[0].getTankType());
        for (int i = 0; i < sending.length; i++) {
            declareFluid(sending[i], recipe == null ? null : recipe.outputFluid[i].type());
        }
        if (recipe == null
                || power < recipe.power
                || tanks[0].getFill() < recipe.inputFluid[0].amount()
                || !inventory.get(SLOT_CATALYST).is(ModItems.CATALYTIC_CONVERTER.get())
                || !recipe.outputFluidsFit(sending)) return;
        recipe.drainInputFluids(receiving);
        setPower(power - recipe.power);
        recipe.fillOutputFluids(sending);
        setChanged();
    }

    private void declareFluid(FluidTankNTM tank, @Nullable Fluid fluid) {
        var previous = tank.getTankType();
        tank.setTankType(fluid);
        if (tank.getTankType() != previous) setChanged();
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long stored) {
        long next = Math.clamp(stored, 0L, MAX_POWER);
        if (power == next) return;
        hbm$syncSet$CatalyticReformerBlockEntity$power(next);
        setChanged();
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
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
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        var faces = FlushFaces.own();
        for (var tank : sending) out.add(tank, faces);
    }

    @Override
    public FluidTankNTM[] settingsTanks() {
        return tanks;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.catalyticReformer");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new CatalyticReformerMenu(id, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$CatalyticReformerBlockEntity$power(input.getLongOr("power", power));
        for (int i = 0; i < tanks.length; i++)
            input.child(TANK_KEYS[i]).ifPresent(tanks[i]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        for (int i = 0; i < tanks.length; i++) tanks[i].serialize(output.child(TANK_KEYS[i]));
    }

    @Override
    public long syncUnitMask() {
        return 3;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> {
                for (var tank : tanks) tank.packetSerialize(output);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$CatalyticReformerBlockEntity$power(input.readLong());
            case 1 -> {
                for (var tank : tanks) tank.packetDeserialize(input);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        for (var tank : tanks) {
            if (tank.getFill() > 0) {
                components.set(
                        ModDataComponents.TANK_CONTENTS.get(), FluidStackNTM.snapshot(tanks));
                return;
            }
        }
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        var contents = components.get(ModDataComponents.TANK_CONTENTS.get());
        if (contents != null) FluidStackNTM.restore(contents, tanks);
    }

    @Override
    public String[] persistentKeys() {
        return TANK_KEYS;
    }

    public final void hbm$syncSet$CatalyticReformerBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final FluidTankNTM hbm$syncArraySet$CatalyticReformerBlockEntity$tanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.tanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.tanks) {
                syncArrayChanged(array, index, 3, 2);
            }
        }
        return next;
    }

    static {
        com.hbm.packet.SyncBindings.field(CatalyticReformerBlockEntity.class, "tanks", 3, 2);
        com.hbm.inventory.container.SyncedData.fields(CatalyticReformerBlockEntity.class, "power");
    }

    public void stripPersistent(ValueOutput output) {
        for (String key : persistentKeys()) output.discard(key);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        writePersistent(components);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        readPersistent(components);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        stripPersistent(output);
    }
}
