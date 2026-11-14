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
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blockentity.processing.SelectedProcessorSupport;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.PUREXMenu;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.PUREXRecipes;
import com.hbm.items.machine.BlueprintsItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.modules.machine.ModuleMachineBase;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.List;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class PurexBlockEntity extends MachineBlockEntity
        implements EnergyHandler,
                FluidTankHandler,
                FluidTankAccess,
                MenuProvider,
                UpgradeInfoProvider,
                AudioLoop,
                RadioValueProvider,
                SyncUnitSchema {
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_BLUEPRINT = 1;
    public static final int SLOT_UPGRADE_START = 2;
    public static final int SLOT_UPGRADE_END = 3;
    public static final int SLOT_ITEM_IN_START = 4;
    public static final int SLOT_ITEM_IN_END = 6;
    public static final int SLOT_ITEM_OUT_START = 7;
    public static final int SLOT_ITEM_OUT_END = 12;
    public static final int SLOT_COUNT = 13;
    public static final int INPUT_TANK_COUNT = 3;
    public static final long MAX_POWER = 1_000_000L;
    private static final int[] INPUTS = {4, 5, 6};
    private static final int[] OUTPUTS = {7, 8, 9, 10, 11, 12};
    private static final int[] ACCESSIBLE = {4, 5, 6, 7, 8, 9, 10, 11, 12};
    private static final String[] RADIO = {"VAL:progress", "VAL:recipe", "VAL:active"};
    private static final FlushFaces FLUSH = FlushFaces.own();
    public static Consumer<PurexBlockEntity> CLIENT_SOUND;

    public long power;
    public long maxPower;

    {
        hbm$syncSet$PurexBlockEntity$maxPower(MAX_POWER);
    }

    public boolean isProgressing;
    public final ModuleMachineBase module;
    public final FluidTankNTM[] inputTanks = {
        new FluidTankNTM(24_000), new FluidTankNTM(24_000), new FluidTankNTM(24_000)
    };
    public final FluidTankNTM[] outputTanks = {new FluidTankNTM(24_000)};
    private final UpgradeManager upgrades = new UpgradeManager(this);
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    public int anim;
    public int prevAnim;
    public boolean frame;

    public PurexBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PUREX.get(), pos, state, SLOT_COUNT);
        module =
                new ModuleMachineBase(
                        0,
                        this,
                        PUREXRecipes.INSTANCE,
                        this,
                        INPUTS,
                        OUTPUTS,
                        inputTanks,
                        outputTanks);
        for (var tank : inputTanks) tank.bindOwner(this);
        outputTanks[0].bindOwner(this);
    }

    @Override
    public void tickServer() {
        long capacity = SelectedProcessorSupport.capacity(module, maxPower, power, MAX_POWER);
        if (capacity != maxPower) {
            hbm$syncSet$PurexBlockEntity$maxPower(capacity);
            setChanged();
        }
        setPower(power + ItemEnergyTransfer.extract(this, SLOT_BATTERY, maxPower - power, false));
        upgrades.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        module.update(
                SelectedProcessorSupport.speed(upgrades),
                SelectedProcessorSupport.power(upgrades),
                true,
                getItem(SLOT_BLUEPRINT));
        hbm$syncSet$PurexBlockEntity$isProgressing(module.didProcess);
        if (module.markDirty || isProgressing) setChanged();
        flush.provide((ServerLevel) level, this);
        networkPackNT(100);
    }

    @Override
    public void tickClient() {
        prevAnim = anim;
        if (isProgressing) anim++;
        if (TickPhase.every(this, 20)) frame = !level.isEmptyBlock(worldPosition.above(5));
        CLIENT_SOUND.accept(this);
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
        if (power != next) {
            hbm$syncSet$PurexBlockEntity$power(next);
            setChanged();
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return true;
        if (slot == SLOT_BLUEPRINT) return stack.getItem() instanceof BlueprintsItem;
        if (slot >= SLOT_UPGRADE_START && slot <= SLOT_UPGRADE_END)
            return MachineUpgradeItem.isUpgrade(stack);
        return module.isItemValid(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_ITEM_OUT_START && slot <= SLOT_ITEM_OUT_END
                || module.isSlotClogged(slot);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot >= SLOT_ITEM_IN_START
                && slot <= SLOT_ITEM_IN_END
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
        return INPUT_TANK_COUNT + 1;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return index < INPUT_TANK_COUNT ? inputTanks[index] : outputTanks[index - INPUT_TANK_COUNT];
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(outputTanks[0], FLUSH);
    }

    @Override
    public UpgradeLimits upgradeCaps() {
        return SelectedProcessorSupport.UPGRADES;
    }

    @Override
    public void appendUpgradeInfo(UpgradeType type, int tier, List<Component> tooltip) {
        UpgradeInfo.processor(type, tier, tooltip);
    }

    @Override
    public Component getDefaultName() {
        return Component.translatable("container.machinePUREX");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PUREXMenu(id, inventory, this);
    }

    @Override
    public @Nullable SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.CHEMICAL_PLANT_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                1F,
                15F,
                0.75F,
                15);
    }

    @Override
    public String[] getFunctionInfo() {
        return RADIO;
    }

    @Override
    public @Nullable String getRadioValue(String key) {
        return switch (key) {
            case "VAL:progress" -> Integer.toString((int) Math.round(module.progress * 100D));
            case "VAL:recipe" -> module.legacyRecipeName();
            case "VAL:active" -> isProgressing ? "1" : "0";
            default -> null;
        };
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        for (int i = 0; i < inputTanks.length; i++) inputTanks[i].serialize(output.child("i" + i));
        outputTanks[0].serialize(output.child("o0"));
        module.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$PurexBlockEntity$power(input.getLongOr("power", power));
        hbm$syncSet$PurexBlockEntity$maxPower(input.getLongOr("maxPower", maxPower));
        for (int i = 0; i < inputTanks.length; i++) {
            int tank = i;
            input.child("i" + i).ifPresent(value -> inputTanks[tank].deserialize(value));
        }
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
            case 2 -> output.writeBoolean(isProgressing);
            case 3 -> module.serialize(output);
            case 4 -> {
                for (var tank : inputTanks) tank.packetSerialize(output);
            }
            case 5 -> outputTanks[0].packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$PurexBlockEntity$power(input.readLong());
            case 1 -> hbm$syncSet$PurexBlockEntity$maxPower(input.readLong());
            case 2 -> hbm$syncSet$PurexBlockEntity$isProgressing(input.readBoolean());
            case 3 -> module.deserialize(input);
            case 4 -> {
                for (var tank : inputTanks) tank.packetDeserialize(input);
            }
            case 5 -> outputTanks[0].packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$PurexBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$PurexBlockEntity$maxPower(long next) {
        if (this.maxPower != next) {
            this.maxPower = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    public final void hbm$syncSet$PurexBlockEntity$isProgressing(boolean next) {
        if (this.isProgressing != next) {
            this.isProgressing = next;
            if ((4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4);
        }
    }

    public final FluidTankNTM hbm$syncArraySet$PurexBlockEntity$inputTanks(
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

    public final FluidTankNTM hbm$syncArraySet$PurexBlockEntity$outputTanks(
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
        com.hbm.packet.SyncBindings.field(PurexBlockEntity.class, "module", 3, 8);
        com.hbm.packet.SyncBindings.field(PurexBlockEntity.class, "inputTanks", 3, 16);
        com.hbm.packet.SyncBindings.field(PurexBlockEntity.class, "outputTanks", 3, 32);
        com.hbm.inventory.container.SyncedData.fields(PurexBlockEntity.class, "power");
    }

    private SoundHandle hbm$audio;

    public void audioLoop(boolean running, float volume) {
        hbm$audio = AudioLoop.loop(this, hbm$audio, running, volume, 0F, false);
    }

    public void audioLoop(boolean running, float volume, float pitch) {
        hbm$audio = AudioLoop.loop(this, hbm$audio, running, volume, pitch, true);
    }

    private void hbm$audioRemove() {
        audioLoop(false, 0F);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        hbm$audioRemove();
    }
}
