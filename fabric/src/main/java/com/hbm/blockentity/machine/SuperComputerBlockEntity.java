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
import com.hbm.api.energymk2.EnergyItem;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.SuperComputerMenu;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.BlueprintsItem;
import com.hbm.modules.machine.ModuleMachineSuperComputer;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SuperComputerBlockEntity extends MachineBlockEntity
        implements EnergyHandler, FluidTankHandler, MenuProvider, SyncUnitSchema {
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_SCHEMATIC = 1;
    public static final int SLOT_INPUT_START = 2;
    public static final int SLOT_OUTPUT_START = 5;
    public static final int SLOT_COUNT = 8;
    public static final long MIN_POWER = 100_000L;
    private static final int[] INPUT_SLOTS = {2, 3, 4};
    private static final int[] OUTPUT_SLOTS = {5, 6, 7};
    private static final int[] ACCESSIBLE_SLOTS = {2, 3, 4, 5, 6, 7};

    public long power;
    public long maxPower;

    {
        hbm$syncSet$SuperComputerBlockEntity$maxPower(MIN_POWER);
    }

    public boolean didProcess;
    public final ModuleMachineSuperComputer module;
    public final FluidTankNTM[] inputTanks = {new FluidTankNTM(4_000)};
    public final FluidTankNTM[] outputTanks = {new FluidTankNTM(4_000)};

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public SuperComputerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUPERCOMPUTER.get(), pos, state, SLOT_COUNT);
        module =
                new ModuleMachineSuperComputer(
                        this, this, INPUT_SLOTS, OUTPUT_SLOTS, inputTanks, outputTanks);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineSuperComputer");
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long value) {
        hbm$syncSet$SuperComputerBlockEntity$power(Math.max(0L, Math.min(value, maxPower)));
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public int tankCount() {
        return inputTanks.length + outputTanks.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        if (index < inputTanks.length) return inputTanks[index];
        index -= inputTanks.length;
        return outputTanks[index];
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return inputTanks;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return outputTanks;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        for (FluidTankNTM tank : getSendingTanks()) out.add(tank, FlushFaces.own());
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return EnergyItem.matches(stack);
        if (slot == SLOT_SCHEMATIC) return stack.getItem() instanceof BlueprintsItem;
        return module.isItemValid(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_OUTPUT_START || module.isSlotClogged(slot);
    }

    @Override
    public void tickServer() {
        long previousPower = power;
        long previousMaxPower = maxPower;
        double previousProgress = module.progress;
        String previousRecipe = module.getRecipeName();
        GenericRecipe selected = module.getRecipe();
        if (selected != null) hbm$syncSet$SuperComputerBlockEntity$maxPower(selected.power * 100L);
        hbm$syncSet$SuperComputerBlockEntity$maxPower(
                Math.max(Math.max(power, maxPower), MIN_POWER));
        hbm$syncSet$SuperComputerBlockEntity$power(
                (long)
                        (power
                                + (ItemEnergyTransfer.extract(
                                        this, SLOT_BATTERY, maxPower - power, false))));
        module.update(1D, 1D, true, inventory.get(SLOT_SCHEMATIC));
        hbm$syncSet$SuperComputerBlockEntity$didProcess(module.didProcess);
        if (module.markDirty || power != previousPower) setChanged();
        if (maxPower != previousMaxPower
                || module.progress != previousProgress
                || !module.getRecipeName().equals(previousRecipe)) markChanged();
        flush.provide((ServerLevel) level, this);
        networkPackNT(100);
    }

    public void selectRecipe(String recipe) {
        module.setRecipe(recipe);
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new SuperComputerMenu(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power")
                .ifPresent(value -> hbm$syncSet$SuperComputerBlockEntity$power(value));
        input.getLong("maxPower")
                .ifPresent(value -> hbm$syncSet$SuperComputerBlockEntity$maxPower(value));
        input.child("i").ifPresent(inputTanks[0]::deserialize);
        input.child("o").ifPresent(outputTanks[0]::deserialize);
        module.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        inputTanks[0].serialize(output.child("i"));
        outputTanks[0].serialize(output.child("o"));
        module.save(output);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x3fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> output.writeLong(maxPower);
            case 2 -> output.writeBoolean(didProcess);
            case 3 -> module.serialize(output);
            case 4 -> inputTanks[0].packetSerialize(output);
            case 5 -> outputTanks[0].packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$SuperComputerBlockEntity$power(input.readLong());
            case 1 -> hbm$syncSet$SuperComputerBlockEntity$maxPower(input.readLong());
            case 2 -> hbm$syncSet$SuperComputerBlockEntity$didProcess(input.readBoolean());
            case 3 -> module.deserialize(input);
            case 4 -> inputTanks[0].packetDeserialize(input);
            case 5 -> outputTanks[0].packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$SuperComputerBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    public final void hbm$syncSet$SuperComputerBlockEntity$maxPower(long next) {
        if (this.maxPower != next) {
            this.maxPower = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$SuperComputerBlockEntity$didProcess(boolean next) {
        if (this.didProcess != next) {
            this.didProcess = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final FluidTankNTM hbm$syncArraySet$SuperComputerBlockEntity$inputTanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.inputTanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.inputTanks) {
                syncArrayChanged(array, index, 3, 1L << 4);
            }
        }
        return next;
    }

    public final FluidTankNTM hbm$syncArraySet$SuperComputerBlockEntity$outputTanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.outputTanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.outputTanks) {
                syncArrayChanged(array, index, 3, 1L << 5);
            }
        }
        return next;
    }

    static {
        com.hbm.packet.SyncBindings.field(SuperComputerBlockEntity.class, "module", 3, 1L << 3);
        com.hbm.packet.SyncBindings.field(SuperComputerBlockEntity.class, "inputTanks", 3, 1L << 4);
        com.hbm.packet.SyncBindings.field(
                SuperComputerBlockEntity.class, "outputTanks", 3, 1L << 5);
        com.hbm.inventory.container.SyncedData.fields(SuperComputerBlockEntity.class, "power");
    }
}
