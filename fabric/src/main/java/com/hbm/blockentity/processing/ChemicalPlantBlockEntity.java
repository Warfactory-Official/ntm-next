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

package com.hbm.blockentity.processing;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.ChemicalPlantMenu;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.items.tool.SwordMeteoriteItem;
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

public class ChemicalPlantBlockEntity extends MachineBlockEntity
        implements EnergyHandler,
                FluidTankHandler,
                FluidSettingsHandler,
                UpgradeInfoProvider,
                MenuProvider,
                AudioLoop,
                SyncUnitSchema,
                RadioValueProvider,
                RadioControlHandler {

    public static final int SLOT_COUNT = 22;
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_SCHEMATIC = 1;
    public static final int SLOT_UPGRADE_START = 2;
    public static final int SLOT_UPGRADE_END = 3;
    public static final int SLOT_ITEM_IN_START = 4;
    public static final int SLOT_ITEM_OUT_START = 7;
    public static final int SLOT_FLUID_IN_START = 10;
    public static final int SLOT_FLUID_IN_RESULT = 13;
    public static final int SLOT_FLUID_OUT_START = 16;
    public static final int SLOT_FLUID_OUT_RESULT = 19;
    public static final int TANK_COUNT = 3;
    public static final long MAX_POWER = 100_000L;

    private static final int[] INPUT_SLOTS = {4, 5, 6};
    private static final int[] OUTPUT_SLOTS = {7, 8, 9};
    private static final int[] AUTOMATION_SLOTS = {4, 5, 6, 7, 8, 9};
    private static final String[] RADIO = {
        "VAL:progress", "VAL:recipe", "VAL:active", "FUN:setrecipe!name"
    };
    private static final FlushFaces OUTPUT_FACES = FlushFaces.activePlane();

    public static Consumer<ChemicalPlantBlockEntity> CLIENT_SOUND = _ -> {};

    public long power;
    public long maxPower;

    {
        hbm$syncSet$ChemicalPlantBlockEntity$maxPower(MAX_POWER);
    }

    public boolean isProgressing;
    public final ModuleMachineBase module;
    public final FluidTankNTM[] inputTanks = {
        new FluidTankNTM(24_000), new FluidTankNTM(24_000), new FluidTankNTM(24_000)
    };
    public final FluidTankNTM[] outputTanks = {
        new FluidTankNTM(24_000), new FluidTankNTM(24_000), new FluidTankNTM(24_000)
    };
    public final UpgradeManager upgradeManager = new UpgradeManager(this);

    public int anim;
    public int prevAnim;
    public boolean frame;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public ChemicalPlantBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEMICALPLANT.get(), pos, state, SLOT_COUNT);
        for (var tank : inputTanks) tank.bindOwner(this);
        for (var tank : outputTanks) tank.bindOwner(this);
        module =
                new ModuleMachineBase(
                        0,
                        this,
                        ChemicalPlantRecipes.INSTANCE,
                        this,
                        INPUT_SLOTS,
                        OUTPUT_SLOTS,
                        inputTanks,
                        outputTanks);
    }

    @Override
    public void tickServer() {
        var recipe = module.getRecipe();
        long ceiling =
                Math.max(
                        power,
                        Math.max(MAX_POWER, recipe == null ? maxPower : recipe.power * 100L));
        if (maxPower != ceiling) {
            hbm$syncSet$ChemicalPlantBlockEntity$maxPower(ceiling);
            setChanged();
        }
        setPower(power + ItemEnergyTransfer.extract(this, SLOT_BATTERY, maxPower - power, false));
        upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        for (int i = 0; i < TANK_COUNT; i++) {
            if (inputTanks[i].loadTank(
                    SLOT_FLUID_IN_START + i, SLOT_FLUID_IN_RESULT + i, inventory)) setChanged();
            if (outputTanks[i].unloadTank(
                    SLOT_FLUID_OUT_START + i, SLOT_FLUID_OUT_RESULT + i, inventory)) setChanged();
        }
        module.update(
                SelectedProcessorSupport.speed(upgradeManager),
                SelectedProcessorSupport.power(upgradeManager),
                true,
                getItem(SLOT_SCHEMATIC));
        hbm$syncSet$ChemicalPlantBlockEntity$isProgressing(module.didProcess);
        if (isProgressing) {
            SwordMeteoriteItem.upgrade(
                    inventory,
                    SLOT_BATTERY,
                    ModItems.METEORITE_SWORD_MACHINED,
                    ModItems.METEORITE_SWORD_TREATED);
        }
        if (isProgressing || module.markDirty) setChanged();
        flush.provide((ServerLevel) level, this);
        networkPackNT(100);
    }

    @Override
    public void tickClient() {
        if (TickPhase.every(this, 20)) frame = !level.getBlockState(worldPosition.above(3)).isAir();
        prevAnim = anim;
        if (isProgressing) anim++;
        tickSound();
    }

    public void tickSound() {
        CLIENT_SOUND.accept(this);
    }

    @Override
    public @Nullable SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.CHEMICAL_PLANT_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX() + 0.5F,
                worldPosition.getY() + 0.5F,
                worldPosition.getZ() + 0.5F,
                getVolume(1F),
                15F,
                1F,
                20);
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
        if (next == power) return;
        hbm$syncSet$ChemicalPlantBlockEntity$power(next);
        setChanged();
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
        return 2 * TANK_COUNT;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return index < TANK_COUNT ? inputTanks[index] : outputTanks[index - TANK_COUNT];
    }

    @Override
    public void declareFlush(FlushLanes out) {
        for (var tank : outputTanks) out.add(tank, OUTPUT_FACES);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= SLOT_ITEM_IN_START
                && slot < SLOT_ITEM_OUT_START
                && module.isItemValid(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_ITEM_OUT_START && slot < SLOT_FLUID_IN_START
                || module.isSlotClogged(slot);
    }

    public void selectRecipe(String recipe) {
        if (SelectedProcessorSupport.select(module, recipe, false)) setChanged();
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
    public String[] getFunctionInfo() {
        return RADIO;
    }

    @Override
    public @Nullable String getRadioValue(String key) {
        return switch (key) {
            case "VAL:progress" -> Long.toString(Math.round(module.progress * 100D));
            case "VAL:recipe" -> module.legacyRecipeName();
            case "VAL:active" -> isProgressing ? "1" : "0";
            default -> null;
        };
    }

    @Override
    public void runRadioFunction(String key, String[] params) {
        if (key.equals("FUN:setrecipe")
                && params.length == 1
                && SelectedProcessorSupport.select(module, params[0], true)) setChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineChemicalPlant");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ChemicalPlantMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        module.save(output);
        for (int i = 0; i < TANK_COUNT; i++) {
            inputTanks[i].serialize(output.child("i" + i));
            outputTanks[i].serialize(output.child("o" + i));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$ChemicalPlantBlockEntity$power(input.getLongOr("power", 0L));
        hbm$syncSet$ChemicalPlantBlockEntity$maxPower(input.getLongOr("maxPower", MAX_POWER));
        module.load(input);
        for (int i = 0; i < TANK_COUNT; i++) {
            input.child("i" + i).ifPresent(inputTanks[i]::deserialize);
            input.child("o" + i).ifPresent(outputTanks[i]::deserialize);
        }
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 63L;
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
            case 5 -> {
                for (var tank : outputTanks) tank.packetSerialize(output);
            }
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$ChemicalPlantBlockEntity$power(input.readLong());
            case 1 -> hbm$syncSet$ChemicalPlantBlockEntity$maxPower(input.readLong());
            case 2 -> hbm$syncSet$ChemicalPlantBlockEntity$isProgressing(input.readBoolean());
            case 3 -> module.deserialize(input);
            case 4 -> {
                for (var tank : inputTanks) tank.packetDeserialize(input);
            }
            case 5 -> {
                for (var tank : outputTanks) tank.packetDeserialize(input);
            }
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$ChemicalPlantBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$ChemicalPlantBlockEntity$maxPower(long next) {
        if (this.maxPower != next) {
            this.maxPower = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    public final void hbm$syncSet$ChemicalPlantBlockEntity$isProgressing(boolean next) {
        if (this.isProgressing != next) {
            this.isProgressing = next;
            if ((4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4);
        }
    }

    public final FluidTankNTM hbm$syncArraySet$ChemicalPlantBlockEntity$inputTanks(
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

    public final FluidTankNTM hbm$syncArraySet$ChemicalPlantBlockEntity$outputTanks(
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
        com.hbm.packet.SyncBindings.field(ChemicalPlantBlockEntity.class, "module", 3, 8);
        com.hbm.packet.SyncBindings.field(ChemicalPlantBlockEntity.class, "inputTanks", 3, 16);
        com.hbm.packet.SyncBindings.field(ChemicalPlantBlockEntity.class, "outputTanks", 3, 32);
        com.hbm.packet.SyncBindings.field(ChemicalPlantBlockEntity.class, "upgradeManager", 3, 0L);
        com.hbm.inventory.container.SyncedData.fields(ChemicalPlantBlockEntity.class, "power");
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
