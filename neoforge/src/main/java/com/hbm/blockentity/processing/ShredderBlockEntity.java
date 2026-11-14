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
import com.hbm.api.energymk2.EnergyItem;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.MachineSoundProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.ShredderMenu;
import com.hbm.inventory.recipes.ShredderRecipe;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.items.machine.BladesItem;
import com.hbm.util.TickPhase;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class ShredderBlockEntity extends MachineBlockEntity
        implements EnergyHandler, MenuProvider, MachineSoundProvider {
    public static final int SLOT_INPUT_START = 0;
    public static final int SLOT_INPUT_END = 9;
    public static final int SLOT_OUTPUT_START = 9;
    public static final int SLOT_OUTPUT_END = 27;
    public static final int SLOT_BLADE_LEFT = 27;
    public static final int SLOT_BLADE_RIGHT = 28;
    public static final int SLOT_BATTERY = 29;
    public static final int SLOT_COUNT = 30;
    public static final int PROCESSING_SPEED = 60;
    public static final long POWER_PER_TICK = 5;
    public static final long MAX_POWER = 10_000;
    private static final int[] EXPOSED_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
        15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29
    };

    public long power;
    public int progress;
    private final ItemStack[] inputViews = new ItemStack[SLOT_INPUT_END];
    private final ItemStack[] reservations = new ItemStack[SLOT_INPUT_END];
    private final ShredderRecipe[] selected = new ShredderRecipe[SLOT_INPUT_END];
    private @Nullable List<ShredderRecipe> recipeRows;

    public ShredderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_SHREDDER.get(), pos, state, SLOT_COUNT);
        Arrays.fill(inputViews, ItemStack.EMPTY);
        Arrays.fill(reservations, ItemStack.EMPTY);
    }

    @Override
    public void tickServer() {
        if (power > 0 && canProcess()) {
            if (progress == 0 || TickPhase.every(this, 50)) {
                level.playSound(
                        null,
                        worldPosition,
                        SoundEvents.MINECART_INSIDE,
                        SoundSource.BLOCKS,
                        getVolume(1),
                        0.75F);
            }
            power = Math.max(0, power - POWER_PER_TICK);
            if (++progress == PROCESSING_SPEED) {
                progress = 0;
                process();
            }
            setChanged();
        } else {
            progress = 0;
        }
        long received = ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);
        if (received != 0) {
            power += received;
            setChanged();
        }
    }

    private void refreshRecipes() {
        var current = ShredderRecipes.INSTANCE.recipes();
        boolean reloaded = current != recipeRows;
        recipeRows = current;
        for (int slot = SLOT_INPUT_START; slot < SLOT_INPUT_END; slot++) {
            var input = inventory.get(slot);
            if (!reloaded && ItemStack.matches(inputViews[slot], input)) continue;
            inputViews[slot] = input.copy();
            var recipe = ShredderRecipes.getRecipe(input);
            selected[slot] = recipe;
            reservations[slot] =
                    input.isEmpty()
                            ? ItemStack.EMPTY
                            : recipe == null
                                    ? ShredderRecipes.getShredderResult(input)
                                    : recipe.output();
        }
    }

    public boolean canProcess() {
        if (!usable(inventory.get(SLOT_BLADE_LEFT)) || !usable(inventory.get(SLOT_BLADE_RIGHT)))
            return false;
        refreshRecipes();
        for (int slot = SLOT_INPUT_START; slot < SLOT_INPUT_END; slot++) {
            var input = inventory.get(slot);
            if (input.isEmpty()) continue;
            if (input.getItem() instanceof Wrecker wrecker) {
                wrecker.wreck(level, worldPosition);
                return false;
            }
            if (eligible(slot)) return true;
        }
        return false;
    }

    private boolean eligible(int slot) {
        return !inventory.get(slot).isEmpty()
                && (selected[slot] != null || !reservations[slot].isEmpty())
                && fits(reservations[slot]);
    }

    public boolean hasSpace(ItemStack input) {
        var recipe = ShredderRecipes.getRecipe(input);
        var maximum = recipe == null ? ShredderRecipes.getShredderResult(input) : recipe.output();
        return !input.isEmpty() && (recipe != null || !maximum.isEmpty()) && fits(maximum);
    }

    private boolean fits(ItemStack product) {
        int remaining = product.getCount();
        if (remaining == 0) return true;
        for (int slot = SLOT_OUTPUT_START; slot < SLOT_OUTPUT_END; slot++) {
            var stored = inventory.get(slot);
            if (stored.isEmpty()) remaining -= product.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(stored, product)) {
                remaining -= Math.max(0, stored.getMaxStackSize() - stored.getCount());
            }
            if (remaining <= 0) return true;
        }
        return false;
    }

    public void process() {
        refreshRecipes();
        wear(SLOT_BLADE_LEFT);
        wear(SLOT_BLADE_RIGHT);
        for (int input = SLOT_INPUT_START; input < SLOT_INPUT_END; input++) {
            if (!eligible(input)) continue;
            var recipe = selected[input];
            var product = recipe == null ? reservations[input].copy() : recipe.roll();
            inventory.get(input).shrink(1);
            for (int slot = SLOT_OUTPUT_START;
                    !product.isEmpty() && slot < SLOT_OUTPUT_END;
                    slot++) {
                var stored = inventory.get(slot);
                if (stored.isEmpty() || !ItemStack.isSameItemSameComponents(stored, product))
                    continue;
                int moved =
                        Math.min(
                                product.getCount(),
                                Math.max(0, stored.getMaxStackSize() - stored.getCount()));
                stored.grow(moved);
                product.shrink(moved);
            }
            for (int slot = SLOT_OUTPUT_START;
                    !product.isEmpty() && slot < SLOT_OUTPUT_END;
                    slot++) {
                if (!inventory.get(slot).isEmpty()) continue;
                int moved = Math.min(product.getCount(), product.getMaxStackSize());
                inventory.set(slot, product.copyWithCount(moved));
                product.shrink(moved);
            }
        }
        setChanged();
    }

    private void wear(int slot) {
        var blade = inventory.get(slot);
        if (blade.isDamageableItem()) blade.setDamageValue(blade.getDamageValue() + 1);
    }

    private static boolean usable(ItemStack stack) {
        int gear = gearOf(stack);
        return gear == 1 || gear == 2;
    }

    public static int gearOf(ItemStack stack) {
        if (!(stack.getItem() instanceof BladesItem)) return 0;
        if (!stack.isDamageableItem()) return 1;
        int damage = stack.getDamageValue();
        if (damage >= stack.getMaxDamage()) return 3;
        return damage < stack.getMaxDamage() / 2 ? 1 : 2;
    }

    public int getGearLeft() {
        return gearOf(inventory.get(SLOT_BLADE_LEFT));
    }

    public int getGearRight() {
        return gearOf(inventory.get(SLOT_BLADE_RIGHT));
    }

    public RandomSource getRandom() {
        return level.getRandom();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return EXPOSED_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < SLOT_INPUT_END) return !(stack.getItem() instanceof BladesItem);
        if (slot == SLOT_BLADE_LEFT || slot == SLOT_BLADE_RIGHT)
            return stack.getItem() instanceof BladesItem;
        return slot == SLOT_BATTERY && EnergyItem.matches(stack);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot == SLOT_BATTERY || !canPlaceItem(slot, stack)) return false;
        var destination = inventory.get(slot);
        if (destination.isEmpty()) return true;
        for (int input = SLOT_INPUT_START; input < SLOT_INPUT_END; input++) {
            var peer = inventory.get(input);
            if (peer.isEmpty()
                    || ItemStack.isSameItemSameComponents(peer, destination)
                            && peer.getCount() < destination.getCount()) return false;
        }
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_OUTPUT_START && slot < SLOT_OUTPUT_END
                || (slot == SLOT_BLADE_LEFT || slot == SLOT_BLADE_RIGHT)
                        && stack.isDamageableItem()
                        && stack.getDamageValue() == stack.getMaxDamage();
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long stored) {
        power = stored;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineShredder");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ShredderMenu(id, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("powerTime", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("powerTime", power);
    }

    public interface Wrecker {
        void wreck(Level level, BlockPos pos);
    }

    static {
        com.hbm.inventory.container.SyncedData.fields(
                ShredderBlockEntity.class, "power", "progress");
    }
}
