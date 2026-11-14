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
import com.hbm.blockentity.SmeltingFurnaceBlockEntity;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.container.ElectricFurnaceMenu;
import com.hbm.inventory.recipes.ingredient.RecipeInputState;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.util.TickPhase;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class ElectricFurnaceBlockEntity extends SmeltingFurnaceBlockEntity
        implements EnergyHandler, UpgradeInfoProvider, MenuProvider, SyncUnitSchema {
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_INPUT = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_UPGRADE = 3;
    public static final int SLOT_COUNT = 4;
    public static final long MAX_POWER = 100_000;
    public static final int BASE_PROCESS_TIME = 100;
    public static final UpgradeLimits VALID_UPGRADES =
            UpgradeLimits.of(UpgradeType.SPEED, 3).and(UpgradeType.POWER, 3);
    private static final int[] AUTOMATED_SLOTS = {SLOT_BATTERY, SLOT_INPUT, SLOT_OUTPUT};

    public long power;
    public int progress;
    public int maxProgress;

    {
        hbm$syncSet$ElectricFurnaceBlockEntity$maxProgress(BASE_PROCESS_TIME);
    }

    private int restartDelay;
    private final UpgradeManager upgrades = new UpgradeManager(this);
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> recipeCheck =
            RecipeManager.createCheck(RecipeType.SMELTING);
    private SingleRecipeInput recipeInput = new SingleRecipeInput(ItemStack.EMPTY);
    private @Nullable RecipeHolder<SmeltingRecipe> assembledRecipe;
    private ItemStack assembledResult = ItemStack.EMPTY;

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_FURNACE.get(), pos, state, SLOT_COUNT, SLOT_OUTPUT);
    }

    @Override
    public void tickServer() {
        if (restartDelay > 0) restartDelay--;
        long received = ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);
        if (received != 0) setPower(power + received);
        upgrades.scan(this, SLOT_UPGRADE, SLOT_UPGRADE);
        int speed = upgrades.getLevel(UpgradeType.SPEED);
        int efficiency = upgrades.getLevel(UpgradeType.POWER);
        int cost = 50 + 50 * speed - 15 * efficiency;
        hbm$syncSet$ElectricFurnaceBlockEntity$maxProgress(
                BASE_PROCESS_TIME - 25 * speed + 10 * efficiency);
        if (power < cost) restartDelay = 20;

        var server = (ServerLevel) level;
        var recipe = restartDelay == 0 ? currentRecipe(server) : null;
        boolean worked = recipe != null && outputFits(assembledResult);
        boolean lit = false;
        if (worked) {
            setPower(power - cost);
            hbm$syncSet$ElectricFurnaceBlockEntity$progress((int) (progress + 1));
            markChanged();
            if (TickPhase.every(this, 20))
                PollutionHandler.add(
                        level, worldPosition, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND);
            if (progress >= maxProgress) {
                var output = getItem(SLOT_OUTPUT);
                if (output.isEmpty()) inventory.set(SLOT_OUTPUT, assembledResult.copy());
                else output.grow(assembledResult.getCount());
                getItem(SLOT_INPUT).shrink(1);
                recipesUsed.record(0, recipe);
                hbm$syncSet$ElectricFurnaceBlockEntity$progress(0);
                markChanged();
                lit =
                        power >= cost
                                && currentRecipe(server) != null
                                && outputFits(assembledResult)
                                && getBlockState().getValue(BlockStateProperties.LIT);
            } else {
                lit = true;
            }
        } else if (progress != 0) {
            hbm$syncSet$ElectricFurnaceBlockEntity$progress(0);
            markChanged();
        }
        var state = getBlockState();
        if (state.getValue(BlockStateProperties.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(BlockStateProperties.LIT, lit), 3);
        }
        networkPackNT(50);
    }

    private @Nullable RecipeHolder<SmeltingRecipe> currentRecipe(ServerLevel server) {
        var input = getItem(SLOT_INPUT);
        if (input.isEmpty()) return null;
        if (recipeInput.item() != input) recipeInput = new SingleRecipeInput(input);
        var holder = recipeCheck.getRecipeFor(recipeInput, server).orElse(null);
        if (holder == null || !RecipeInputState.accepts(holder.value().input(), input)) return null;
        if (holder != assembledRecipe || holder.value().getClass() != SmeltingRecipe.class) {
            assembledResult = holder.value().assemble(recipeInput);
            assembledRecipe = holder;
        }
        return assembledResult.isEmpty() ? null : holder;
    }

    private boolean outputFits(ItemStack result) {
        var held = getItem(SLOT_OUTPUT);
        int limit = Math.min(getMaxStackSize(), result.getMaxStackSize());
        return held.isEmpty()
                ? result.getCount() <= limit
                : ItemStack.isSameItemSameComponents(held, result)
                        && held.getCount() + result.getCount() <= limit;
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
        long next = Math.clamp(stored, 0, MAX_POWER);
        if (power == next) return;
        power = next;
        markChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATED_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return EnergyItem.matches(stack);
        if (slot != SLOT_INPUT || stack.isEmpty() || !(level instanceof ServerLevel server))
            return false;
        var recipe = recipeCheck.getRecipeFor(new SingleRecipeInput(stack), server).orElse(null);
        return recipe != null && RecipeInputState.accepts(recipe.value().input(), stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT
                || slot == SLOT_BATTERY
                        && stack.getItem() instanceof EnergyItem battery
                        && battery.getCharge(stack) == 0;
    }

    @Override
    public UpgradeLimits upgradeCaps() {
        return VALID_UPGRADES;
    }

    @Override
    public void appendUpgradeInfo(UpgradeType type, int level, List<Component> tooltip) {
        UpgradeInfo.electricFurnace(type, level, tooltip);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.electricFurnace");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ElectricFurnaceMenu(id, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", 0);
        hbm$syncSet$ElectricFurnaceBlockEntity$progress(input.getIntOr("progress", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 3;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(progress);
            case 1 -> output.writeInt(maxProgress);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$ElectricFurnaceBlockEntity$progress(input.readInt());
            case 1 -> hbm$syncSet$ElectricFurnaceBlockEntity$maxProgress(input.readInt());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$ElectricFurnaceBlockEntity$progress(int next) {
        if (this.progress != next) {
            this.progress = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$ElectricFurnaceBlockEntity$maxProgress(int next) {
        if (this.maxProgress != next) {
            this.maxProgress = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    static {
        com.hbm.inventory.container.SyncedData.fields(
                ElectricFurnaceBlockEntity.class, "maxProgress", "power", "progress");
    }
}
