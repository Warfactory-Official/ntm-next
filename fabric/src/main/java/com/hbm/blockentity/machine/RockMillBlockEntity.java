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
import com.hbm.blockentity.MachineSoundProvider;
import com.hbm.blockentity.processing.SelectedProcessorSupport;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.RockMillMenu;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.BlueprintsItem;
import com.hbm.modules.machine.ModuleMachineRockMill;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.WorldSounds;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class RockMillBlockEntity extends MachineBlockEntity
        implements EnergyHandler,
                FluidTankHandler,
                FluidTankAccess,
                MachineSoundProvider,
                MenuProvider,
                SyncUnitSchema {
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_SCHEMATIC = 1;
    public static final int SLOT_INPUT_START = 2;
    public static final int SLOT_INPUT_END = 4;
    public static final int SLOT_OUTPUT_START = 5;
    public static final int SLOT_OUTPUT_END = 7;
    public static final int SLOT_COUNT = 8;
    public static final long MIN_POWER = 2_500L;
    private static final int[] INPUTS = {2, 3, 4};
    private static final int[] OUTPUTS = {5, 6, 7};
    private static final int[] ACCESSIBLE = {2, 3, 4, 5, 6, 7};
    private static final FlushFaces FLUSH = FlushFaces.own();
    public static Consumer<RockMillBlockEntity> CLIENT_DUST;

    public long power;
    public long maxPower;

    {
        hbm$syncSet$RockMillBlockEntity$maxPower(MIN_POWER);
    }

    public boolean didProcess;
    public final ModuleMachineRockMill module;
    public final FluidTankNTM[] inputTanks = {new FluidTankNTM(4_000)};
    public final FluidTankNTM[] outputTanks = {new FluidTankNTM(4_000)};
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    public float rotation;
    public float prevRotation;
    public float rotationSpeed;
    public boolean frame;

    public RockMillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROCK_MILL.get(), pos, state, SLOT_COUNT);
        module = new ModuleMachineRockMill(this, this, INPUTS, OUTPUTS, inputTanks, outputTanks);
        inputTanks[0].bindOwner(this);
        outputTanks[0].bindOwner(this);
    }

    @Override
    public void tickServer() {
        long capacity = SelectedProcessorSupport.capacity(module, maxPower, power, MIN_POWER);
        if (maxPower != capacity) {
            hbm$syncSet$RockMillBlockEntity$maxPower(capacity);
            setChanged();
        }
        setPower(power + ItemEnergyTransfer.extract(this, SLOT_BATTERY, maxPower - power, false));
        module.update(1D, 1D, true, getItem(SLOT_SCHEMATIC));
        hbm$syncSet$RockMillBlockEntity$didProcess(module.didProcess);
        if (module.markDirty || didProcess) setChanged();
        if (didProcess && TickPhase.every(this, 3)) {
            var recipe = module.getRecipe();
            var block =
                    recipe != null && recipe.getIcon().getItem() instanceof BlockItem item
                            ? item.getBlock()
                            : Blocks.STONE;
            WorldSounds.machine(
                    level,
                    null,
                    worldPosition,
                    block.defaultBlockState().getSoundType().getStepSound(),
                    SoundSource.BLOCKS,
                    getVolume(1F),
                    0.75F);
        }
        flush.provide((ServerLevel) level, this);
        networkPackNT(100);
    }

    @Override
    public void tickClient() {
        prevRotation = rotation;
        rotationSpeed = Math.clamp(rotationSpeed + (didProcess ? 0.1F : -0.1F), 0F, 15F);
        rotation += rotationSpeed;
        if (rotation >= 360F) {
            rotation -= 360F;
            prevRotation -= 360F;
        }
        if (TickPhase.every(this, 20)) frame = !level.isEmptyBlock(worldPosition.above(3));
        if (didProcess) CLIENT_DUST.accept(this);
    }

    public void selectRecipe(String recipe) {
        if (SelectedProcessorSupport.select(module, recipe, false)) setChanged();
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public void setPower(long stored) {
        long next = Math.clamp(stored, 0L, maxPower);
        if (next != power) {
            hbm$syncSet$RockMillBlockEntity$power(next);
            setChanged();
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return EnergyItem.matches(stack);
        if (slot == SLOT_SCHEMATIC) return stack.getItem() instanceof BlueprintsItem;
        return module.isItemValid(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_OUTPUT_START && slot <= SLOT_OUTPUT_END || module.isSlotClogged(slot);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot >= SLOT_INPUT_START
                && slot <= SLOT_INPUT_END
                && module.isItemValid(slot, stack);
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
    public int tankCount() {
        return 2;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return index == 0 ? inputTanks[0] : outputTanks[index - 1];
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(outputTanks[0], FLUSH);
    }

    @Override
    public Component getDefaultName() {
        return Component.translatable("container.machineRockMill");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RockMillMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        inputTanks[0].serialize(output.child("i0"));
        outputTanks[0].serialize(output.child("o0"));
        module.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$RockMillBlockEntity$power(input.getLongOr("power", power));
        hbm$syncSet$RockMillBlockEntity$maxPower(input.getLongOr("maxPower", maxPower));
        input.child("i0").ifPresent(inputTanks[0]::deserialize);
        input.child("o0").ifPresent(outputTanks[0]::deserialize);
        module.load(input);
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
            case 0 -> hbm$syncSet$RockMillBlockEntity$power(input.readLong());
            case 1 -> hbm$syncSet$RockMillBlockEntity$maxPower(input.readLong());
            case 2 -> hbm$syncSet$RockMillBlockEntity$didProcess(input.readBoolean());
            case 3 -> module.deserialize(input);
            case 4 -> inputTanks[0].packetDeserialize(input);
            case 5 -> outputTanks[0].packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$RockMillBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$RockMillBlockEntity$maxPower(long next) {
        if (this.maxPower != next) {
            this.maxPower = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    public final void hbm$syncSet$RockMillBlockEntity$didProcess(boolean next) {
        if (this.didProcess != next) {
            this.didProcess = next;
            if ((4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4);
        }
    }

    public final FluidTankNTM hbm$syncArraySet$RockMillBlockEntity$inputTanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.inputTanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.inputTanks) {
                syncArrayChanged(array, index, 3, 16);
            }
        }
        return next;
    }

    public final FluidTankNTM hbm$syncArraySet$RockMillBlockEntity$outputTanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.outputTanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.outputTanks) {
                syncArrayChanged(array, index, 3, 32);
            }
        }
        return next;
    }

    static {
        com.hbm.packet.SyncBindings.field(RockMillBlockEntity.class, "module", 3, 8);
        com.hbm.packet.SyncBindings.field(RockMillBlockEntity.class, "inputTanks", 3, 16);
        com.hbm.packet.SyncBindings.field(RockMillBlockEntity.class, "outputTanks", 3, 32);
        com.hbm.inventory.container.SyncedData.fields(RockMillBlockEntity.class, "power");
    }
}
