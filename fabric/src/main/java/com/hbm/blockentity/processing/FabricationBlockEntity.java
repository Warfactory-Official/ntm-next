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

package com.hbm.blockentity.processing;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.Outputs;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public abstract class FabricationBlockEntity extends MachineBlockEntity
        implements EnergyHandler,
                FluidTankHandler,
                FluidSettingsHandler,
                UpgradeInfoProvider,
                MenuProvider,
                SyncUnitSchema {
    private static final UpgradeLimits UPGRADES =
            UpgradeLimits.of(UpgradeType.SPEED, 3)
                    .and(UpgradeType.POWER, 3)
                    .and(UpgradeType.OVERDRIVE, 3);

    public long power;
    public long maxPower;

    {
        hbm$syncSet$FabricationBlockEntity$maxPower(2_000);
    }

    public long consumption;
    public int progress;
    public int processTime;

    {
        hbm$syncSet$FabricationBlockEntity$processTime(1);
    }

    public ItemStack display;

    {
        hbm$syncSet$FabricationBlockEntity$display(ItemStack.EMPTY);
    }

    public final FluidTankNTM tank;
    public final UpgradeManager upgradeManager = new UpgradeManager(this);

    private final FluidTankNTM[] tanks;
    private final int[] recipeSlots;
    private final ItemStack[] previousInputs;
    private final int batterySlot;
    private final int identifierSlot;
    private final int outputSlot;
    private final int firstUpgrade;
    private @Nullable List<? extends GenericRecipe> previousRecipes;
    protected @Nullable GenericRecipe recipe;
    private int workStep = 1;

    protected FabricationBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            int slots,
            int tankCapacity,
            int[] recipeSlots,
            int batterySlot,
            int identifierSlot,
            int outputSlot,
            int firstUpgrade) {
        super(type, pos, state, slots);
        tank = new FluidTankNTM(tankCapacity);
        tank.bindOwner(this);
        tanks = new FluidTankNTM[] {tank};
        this.recipeSlots = recipeSlots;
        previousInputs = new ItemStack[recipeSlots.length];
        Arrays.fill(previousInputs, ItemStack.EMPTY);
        this.batterySlot = batterySlot;
        this.identifierSlot = identifierSlot;
        this.outputSlot = outputSlot;
        this.firstUpgrade = firstUpgrade;
    }

    protected abstract List<? extends GenericRecipe> recipes();

    protected abstract @Nullable GenericRecipe selectRecipe();

    protected abstract @Nullable FluidStackNTM recipeFluid();

    protected abstract Item previewItem();

    protected abstract ItemStack craftProduct();

    protected abstract void consumeIngredients();

    protected abstract void emitWorkEffects();

    protected boolean allowsFluidFreeWork() {
        return true;
    }

    protected final void tickFabrication() {
        long oldPower = power;
        long oldCapacity = maxPower;
        long oldCost = consumption;
        int oldProgress = progress;
        int oldDuration = processTime;
        hbm$syncSet$FabricationBlockEntity$power(
                (long)
                        (power
                                + (ItemEnergyTransfer.extract(
                                        this, batterySlot, maxPower - power, false))));
        boolean configured = tank.setType(identifierSlot, identifierSlot, inventory);
        var rows = recipes();
        if (inputsChanged(rows)) {
            upgradeManager.scan(this, firstUpgrade, firstUpgrade + 1);
            recipe = selectRecipe();
            workStep = 1 + upgradeManager.getLevel(UpgradeType.OVERDRIVE);
            if (recipe == null) {
                hbm$syncSet$FabricationBlockEntity$consumption(100);
            } else {
                int speed = upgradeManager.getLevel(UpgradeType.SPEED);
                int saving = upgradeManager.getLevel(UpgradeType.POWER);
                int duration = recipe.duration;
                long cost = recipe.power;
                hbm$syncSet$FabricationBlockEntity$processTime(
                        duration - duration * speed / 6 + duration * saving / 3);
                hbm$syncSet$FabricationBlockEntity$consumption(
                        (cost + cost * speed - cost * saving / 6)
                                * (1L << upgradeManager.getLevel(UpgradeType.OVERDRIVE)));
            }
            refreshDisplay();
        }
        hbm$syncSet$FabricationBlockEntity$maxPower(
                Math.max(power, recipe == null ? 2_000 : consumption * 20));
        boolean crafted = false;
        if (ready()) {
            hbm$syncSet$FabricationBlockEntity$power((long) (power - (consumption)));
            hbm$syncSet$FabricationBlockEntity$progress((int) (progress + (workStep)));
            emitWorkEffects();
            if (progress >= processTime) {
                hbm$syncSet$FabricationBlockEntity$progress(0);
                var product = craftProduct();
                consumeIngredients();
                var fluid = recipeFluid();
                if (fluid != null) fluid.drainFrom(tank);
                var stored = inventory.get(outputSlot);
                if (stored.isEmpty()) inventory.set(outputSlot, product.copy());
                else stored.grow(product.getCount());
                crafted = true;
            }
        } else {
            hbm$syncSet$FabricationBlockEntity$progress(0);
        }
        if (configured
                || crafted
                || power != oldPower
                || maxPower != oldCapacity
                || consumption != oldCost
                || progress != oldProgress
                || processTime != oldDuration) setChanged();
        networkPackNT(25);
    }

    private boolean inputsChanged(List<? extends GenericRecipe> rows) {
        boolean changed = previousRecipes != rows;
        previousRecipes = rows;
        for (int index = 0; index < recipeSlots.length; index++) {
            var stack = inventory.get(recipeSlots[index]);
            if (!ItemStack.matches(previousInputs[index], stack)) {
                previousInputs[index] = stack.copy();
                changed = true;
            }
        }
        return changed;
    }

    private void refreshDisplay() {
        if (recipe == null) {
            hbm$syncSet$FabricationBlockEntity$display(ItemStack.EMPTY);
        } else {
            var item = previewItem();
            if (!display.is(item)) hbm$syncSet$FabricationBlockEntity$display(new ItemStack(item));
        }
    }

    private boolean ready() {
        if (recipe == null
                || power < consumption
                || !Outputs.fits(inventory.get(outputSlot), recipe.outputItems(), 0)) return false;
        var fluid = recipeFluid();
        return fluid == null
                ? allowsFluidFreeWork()
                : tank.provides(fluid.type(), fluid.components())
                        && tank.getPressure() == fluid.pressure()
                        && tank.getFill() >= fluid.amount();
    }

    protected final void consumeGroup(CountIngredient[] ingredients, int[] slots) {
        for (var ingredient : ingredients) {
            for (int slot : slots) {
                var stack = inventory.get(slot);
                if (ingredient.test(stack)) {
                    stack.shrink(ingredient.count());
                    break;
                }
            }
        }
    }

    public final RandomSource getRandom() {
        return level.getRandom();
    }

    @Override
    public final long getPower() {
        return Math.clamp(power, 0, maxPower);
    }

    @Override
    public final void setPower(long stored) {
        if (power != stored) {
            hbm$syncSet$FabricationBlockEntity$power(stored);
            setChanged();
        }
    }

    @Override
    public final long getMaxPower() {
        return maxPower;
    }

    @Override
    public final int tankCount() {
        return 1;
    }

    @Override
    public final FluidTankNTM tank(int index) {
        return tanks[index];
    }

    @Override
    public final FluidTankNTM[] getReceivingTanks() {
        return tanks;
    }

    @Override
    public final FluidTankNTM[] settingsTanks() {
        return tanks;
    }

    @Override
    public final FluidTankNTM pasteTank() {
        return tank;
    }

    @Override
    public final UpgradeLimits upgradeCaps() {
        return UPGRADES;
    }

    @Override
    public final void appendUpgradeInfo(UpgradeType type, int level, List<Component> tooltip) {
        UpgradeInfo.soldering(type, level, tooltip);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$FabricationBlockEntity$power(input.getLongOr("power", power));
        hbm$syncSet$FabricationBlockEntity$maxPower(input.getLongOr("maxPower", maxPower));
        hbm$syncSet$FabricationBlockEntity$consumption(input.getLongOr("consumption", consumption));
        hbm$syncSet$FabricationBlockEntity$progress(input.getIntOr("progress", progress));
        hbm$syncSet$FabricationBlockEntity$processTime(input.getIntOr("processTime", processTime));
        input.child("tank").ifPresent(tank::deserialize);
        previousRecipes = null;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        output.putLong("consumption", consumption);
        output.putInt("progress", progress);
        output.putInt("processTime", processTime);
        tank.serialize(output.child("tank"));
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> output.writeLong(maxPower);
            case 2 -> output.writeLong(consumption);
            case 3 -> output.writeInt(progress);
            case 4 -> output.writeInt(processTime);
            case 6 -> {
                output.writeBoolean(!display.isEmpty());
                if (!display.isEmpty())
                    output.writeInt(BuiltInRegistries.ITEM.getId(display.getItem()));
            }
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$FabricationBlockEntity$power(input.readLong());
            case 1 -> hbm$syncSet$FabricationBlockEntity$maxPower(input.readLong());
            case 2 -> hbm$syncSet$FabricationBlockEntity$consumption(input.readLong());
            case 3 -> hbm$syncSet$FabricationBlockEntity$progress(input.readInt());
            case 4 -> hbm$syncSet$FabricationBlockEntity$processTime(input.readInt());
            case 6 ->
                    hbm$syncSet$FabricationBlockEntity$display(
                            input.readBoolean()
                                    ? new ItemStack(BuiltInRegistries.ITEM.byId(input.readInt()))
                                    : ItemStack.EMPTY);
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$FabricationBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$FabricationBlockEntity$maxPower(long next) {
        if (this.maxPower != next) {
            this.maxPower = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    public final void hbm$syncSet$FabricationBlockEntity$consumption(long next) {
        if (this.consumption != next) {
            this.consumption = next;
            if ((4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4);
        }
    }

    public final void hbm$syncSet$FabricationBlockEntity$progress(int next) {
        if (this.progress != next) {
            this.progress = next;
            if ((8) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 8);
        }
    }

    public final void hbm$syncSet$FabricationBlockEntity$processTime(int next) {
        if (this.processTime != next) {
            this.processTime = next;
            if ((16) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 16);
        }
    }

    public final void hbm$syncSet$FabricationBlockEntity$display(ItemStack next) {
        if (this.display != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.display);
            this.display = next;
            if (bound) bindSyncValue(next, 3, 64);
            if ((64) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 64);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(FabricationBlockEntity.class, "display", 3, 64);
        com.hbm.packet.SyncBindings.field(FabricationBlockEntity.class, "tank", 3, 0L);
        com.hbm.inventory.container.SyncedData.fields(
                FabricationBlockEntity.class,
                "consumption",
                "maxPower",
                "power",
                "processTime",
                "progress");
    }
}
