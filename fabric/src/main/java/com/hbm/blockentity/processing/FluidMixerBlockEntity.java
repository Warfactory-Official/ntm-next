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

package com.hbm.blockentity.processing;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MixerMenu;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.MixerRecipe;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import java.util.List;
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
import org.jspecify.annotations.Nullable;

public class FluidMixerBlockEntity extends MachineBlockEntity
        implements EnergyHandler,
                FluidTankHandler,
                FluidSettingsHandler,
                MenuProvider,
                UpgradeInfoProvider,
                SyncUnitSchema {
    public static final int SLOT_COUNT = 5;
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_ITEM_INPUT = 1;
    public static final int SLOT_FLUID_ID = 2;
    public static final int SLOT_UPGRADE_START = 3;
    public static final int SLOT_UPGRADE_END = 4;
    public static final int TANK_IN1 = 0;
    public static final int TANK_IN2 = 1;
    public static final int TANK_OUT = 2;
    public static final long MAX_POWER = 10_000L;
    public static final UpgradeLimits VALID_UPGRADES =
            UpgradeLimits.of(UpgradeType.SPEED, 3)
                    .and(UpgradeType.POWER, 3)
                    .and(UpgradeType.OVERDRIVE, 6);
    private static final int[] ITEM_ACCESS = {SLOT_ITEM_INPUT};
    public final FluidTankNTM[] tanks = {
        new FluidTankNTM(16_000), new FluidTankNTM(16_000), new FluidTankNTM(24_000)
    };
    private final FluidTankNTM[] inputs = {tanks[TANK_IN1], tanks[TANK_IN2]};
    private final FluidTankNTM[] outputs = {tanks[TANK_OUT]};
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final UpgradeManager upgrades = new UpgradeManager(this);
    public long power;
    public int processTime;
    public int progress;
    public int recipeIndex;
    public boolean isProgressing;
    public float rotation;
    public float prevRotation;

    public FluidMixerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MIXER.get(), pos, state, SLOT_COUNT);
        for (var tank : tanks) tank.bindOwner(this);
    }

    private @Nullable MixerRecipe selectedRecipe() {
        var candidates = MixerRecipes.INSTANCE.getOutput(tanks[TANK_OUT].getDeclaredFluid());
        if (candidates.isEmpty()) {
            if (recipeIndex != 0) setChanged();
            hbm$syncSet$FluidMixerBlockEntity$recipeIndex(0);
            return null;
        }
        int previous = recipeIndex;
        hbm$syncSet$FluidMixerBlockEntity$recipeIndex((int) (recipeIndex % (candidates.size())));
        if (recipeIndex != previous) setChanged();
        return candidates.get(recipeIndex);
    }

    private void declareInputs(MixerRecipe recipe) {
        for (int i = 0; i < inputs.length; i++) {
            var tank = inputs[i];
            var fluid = i < recipe.inputFluid.length ? recipe.inputFluid[i] : null;
            var previous = tank.getTankType();
            int pressure = tank.getPressure();
            int fill = tank.getFill();
            if (fluid == null) tank.resetTank();
            else tank.conform(fluid);
            if (previous != tank.getTankType()
                    || pressure != tank.getPressure()
                    || fill != tank.getFill()) {
                setChanged();
            }
        }
    }

    private boolean ready(MixerRecipe recipe, int consumption) {
        if (power < consumption
                || !recipe.matchesInputFluids(inputs)
                || !recipe.outputFluidsFit(outputs)) return false;
        var ingredient = recipe.solidInput();
        var input = getItem(SLOT_ITEM_INPUT);
        return ingredient == null
                || ingredient.matchesItem(input) && input.getCount() >= ingredient.count();
    }

    public boolean canProcess() {
        var recipe = selectedRecipe();
        if (recipe == null) return false;
        declareInputs(recipe);
        if (!ready(recipe, getConsumption())) return false;
        hbm$syncSet$FluidMixerBlockEntity$processTime(recipe.duration);
        return true;
    }

    public int getConsumption() {
        int speed = upgrades.getLevel(UpgradeType.SPEED);
        int consumption = 50 + 150 * speed;
        consumption =
                (int) (consumption - consumption * upgrades.getLevel(UpgradeType.POWER) * 0.25D);
        return consumption * (3 * upgrades.getLevel(UpgradeType.OVERDRIVE) + 1);
    }

    @Override
    public void tickServer() {
        long previousPower = power;
        int previousProgress = progress;
        int previousDuration = processTime;
        int previousSelection = recipeIndex;
        hbm$syncSet$FluidMixerBlockEntity$power(
                (long)
                        (power
                                + (ItemEnergyTransfer.extract(
                                        this, SLOT_BATTERY, MAX_POWER - power, false))));
        if (tanks[TANK_OUT].setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory)) setChanged();
        upgrades.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        var recipe = selectedRecipe();
        hbm$syncSet$FluidMixerBlockEntity$isProgressing(false);
        if (recipe != null) {
            declareInputs(recipe);
            int consumption = getConsumption();
            if (ready(recipe, consumption)) {
                int speed = upgrades.getLevel(UpgradeType.SPEED);
                hbm$syncSet$FluidMixerBlockEntity$processTime(
                        Math.max(
                                1,
                                (recipe.duration - recipe.duration * speed / 4)
                                        / (upgrades.getLevel(UpgradeType.OVERDRIVE) + 1)));
                hbm$syncSet$FluidMixerBlockEntity$power((long) (power - (consumption)));
                hbm$syncSet$FluidMixerBlockEntity$progress((int) (progress + 1));
                hbm$syncSet$FluidMixerBlockEntity$isProgressing(true);
                if (progress >= processTime) {
                    recipe.drainInputFluids(inputs);
                    var ingredient = recipe.solidInput();
                    if (ingredient != null) removeItem(SLOT_ITEM_INPUT, ingredient.count());
                    recipe.fillOutputFluids(outputs);
                    hbm$syncSet$FluidMixerBlockEntity$progress(0);
                    setChanged();
                }
            }
        }
        if (!isProgressing) hbm$syncSet$FluidMixerBlockEntity$progress(0);
        flush.provide((ServerLevel) level, this);
        if (power != previousPower
                || progress != previousProgress
                || processTime != previousDuration
                || recipeIndex != previousSelection) setChanged();
        networkPackNT(50);
    }

    public void cycleRecipe() {
        hbm$syncSet$FluidMixerBlockEntity$recipeIndex((int) (recipeIndex + 1));
        setChanged();
    }

    @Override
    public void tickClient() {
        prevRotation = rotation;
        if (isProgressing) {
            rotation += 20F;
            if (rotation >= 360F) {
                rotation -= 360F;
                prevRotation -= 360F;
            }
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ITEM_ACCESS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != SLOT_ITEM_INPUT) return false;
        var recipe = selectedRecipe();
        var ingredient = recipe == null ? null : recipe.solidInput();
        return ingredient != null && ingredient.matchesItem(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return inputs;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return outputs;
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
    public FluidTankNTM[] settingsTanks() {
        return tanks;
    }

    @Override
    public FluidTankNTM pasteTank() {
        return tanks[TANK_OUT];
    }

    @Override
    public void declareFlush(FlushLanes lanes) {
        lanes.add(tanks[TANK_OUT], FlushFaces.sixFaces());
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$FluidMixerBlockEntity$power(Math.clamp(stored, 0L, MAX_POWER));
    }

    @Override
    public UpgradeLimits upgradeCaps() {
        return VALID_UPGRADES;
    }

    @Override
    public void appendUpgradeInfo(UpgradeType type, int tier, List<Component> tooltip) {
        if (type == UpgradeType.SPEED) UpgradeInfo.speedAndPower(tooltip, -25 * tier, 300 * tier);
        else if (type == UpgradeType.POWER)
            UpgradeInfo.percent(tooltip, "upgrade.consumption", -25 * tier, true);
        else if (type == UpgradeType.OVERDRIVE) UpgradeInfo.centrifuge(type, tier, tooltip);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineMixer");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new MixerMenu(id, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$FluidMixerBlockEntity$power(input.getLongOr("power", power));
        hbm$syncSet$FluidMixerBlockEntity$progress(input.getIntOr("progress", progress));
        hbm$syncSet$FluidMixerBlockEntity$processTime(input.getIntOr("processTime", processTime));
        hbm$syncSet$FluidMixerBlockEntity$recipeIndex(input.getIntOr("recipe", recipeIndex));
        if (recipeIndex < 0) throw new IllegalArgumentException("Negative mixer recipe index");
        for (int i = 0; i < tanks.length; i++) {
            int index = i;
            input.child(Integer.toString(i)).ifPresent(child -> tanks[index].deserialize(child));
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
        output.putInt("processTime", processTime);
        output.putInt("recipe", recipeIndex);
        for (int i = 0; i < tanks.length; i++)
            tanks[i].serialize(output.child(Integer.toString(i)));
    }

    @Override
    public long syncUnitMask() {
        return 63L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> output.writeInt(processTime);
            case 2 -> output.writeInt(progress);
            case 3 -> output.writeInt(recipeIndex);
            case 4 -> output.writeBoolean(isProgressing);
            case 5 -> {
                for (var tank : tanks) tank.packetSerialize(output);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$FluidMixerBlockEntity$power(input.readLong());
            case 1 -> hbm$syncSet$FluidMixerBlockEntity$processTime(input.readInt());
            case 2 -> hbm$syncSet$FluidMixerBlockEntity$progress(input.readInt());
            case 3 -> {
                hbm$syncSet$FluidMixerBlockEntity$recipeIndex(input.readInt());
                if (recipeIndex < 0)
                    throw new IllegalArgumentException("Negative mixer recipe index");
            }
            case 4 -> hbm$syncSet$FluidMixerBlockEntity$isProgressing(input.readBoolean());
            case 5 -> {
                for (var tank : tanks) tank.packetDeserialize(input);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    public final FluidTankNTM hbm$syncArraySet$FluidMixerBlockEntity$tanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.tanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.tanks) {
                syncArrayChanged(array, index, 3, 32L);
            }
        }
        return next;
    }

    public final void hbm$syncSet$FluidMixerBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$FluidMixerBlockEntity$processTime(int next) {
        if (this.processTime != next) {
            this.processTime = next;
            if ((2L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2L);
        }
    }

    public final void hbm$syncSet$FluidMixerBlockEntity$progress(int next) {
        if (this.progress != next) {
            this.progress = next;
            if ((4L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4L);
        }
    }

    public final void hbm$syncSet$FluidMixerBlockEntity$recipeIndex(int next) {
        if (this.recipeIndex != next) {
            this.recipeIndex = next;
            if ((8L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 8L);
        }
    }

    public final void hbm$syncSet$FluidMixerBlockEntity$isProgressing(boolean next) {
        if (this.isProgressing != next) {
            this.isProgressing = next;
            if ((16L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 16L);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(FluidMixerBlockEntity.class, "tanks", 3, 32L);
        com.hbm.inventory.container.SyncedData.fields(FluidMixerBlockEntity.class, "power");
    }
}
