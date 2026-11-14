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
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.SmallExplosionEffect;
import com.hbm.explosion.vanillant.standard.WeaponExplosionEntityProcessor;
import com.hbm.inventory.container.MicrowaveMenu;
import com.hbm.inventory.recipes.ingredient.RecipeInputState;
import com.hbm.packet.SyncUnitSchema;
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
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class MicrowaveBlockEntity extends MachineBlockEntity
        implements EnergyHandler, MenuProvider, SyncUnitSchema {
    public static final int SLOT_COUNT = 3;
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int BATTERY_SLOT = 2;
    public static final long ENERGY_CAPACITY = 50_000;
    public static final int PROGRESS_REQUIRED = 300;
    public static final int SPEED_LIMIT = 5;
    public static final int ENERGY_PER_INCREMENT = 50;
    private static final int[] INPUT_ACCESS = {INPUT_SLOT};
    private static final int[] OUTPUT_ACCESS = {OUTPUT_SLOT};

    public long energy;
    public int progress;
    public int speed;
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> recipeHint =
            RecipeManager.createCheck(RecipeType.SMELTING);

    public MicrowaveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MICROWAVE.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickServer() {
        long charged =
                ItemEnergyTransfer.extract(this, BATTERY_SLOT, ENERGY_CAPACITY - energy, false);
        if (charged != 0) {
            hbm$syncSet$MicrowaveBlockEntity$energy((long) (energy + (charged)));
            setChanged();
        }
        var product = eligibleProduct();
        if (!product.isEmpty()) {
            if (speed >= SPEED_LIMIT) {
                level.removeBlock(worldPosition, false);
                new ComposableExplosion(level, worldPosition, 5)
                        .entityProcessor(new WeaponExplosionEntityProcessor(1, 50))
                        .effects(new SmallExplosionEffect(10, 2.5F, 1F))
                        .explode();
                return;
            }
            if (progress >= PROGRESS_REQUIRED) {
                removeItem(INPUT_SLOT, 1);
                var output = getItem(OUTPUT_SLOT);
                if (output.isEmpty()) setItem(OUTPUT_SLOT, product);
                else output.grow(product.getCount());
                hbm$syncSet$MicrowaveBlockEntity$progress(0);
                setChanged();
                product = eligibleProduct();
            }
            if (!product.isEmpty()) {
                hbm$syncSet$MicrowaveBlockEntity$energy((long) (energy - (ENERGY_PER_INCREMENT)));
                hbm$syncSet$MicrowaveBlockEntity$progress((int) (progress + (speed * 2)));
                setChanged();
            }
        }
        networkPackNT(50);
    }

    private ItemStack eligibleProduct() {
        if (speed == 0 || energy < ENERGY_PER_INCREMENT) return ItemStack.EMPTY;
        var input = getItem(INPUT_SLOT);
        var recipeInput = new SingleRecipeInput(input);
        var selected = recipeHint.getRecipeFor(recipeInput, (ServerLevel) level).orElse(null);
        if (selected == null || !RecipeInputState.accepts(selected.value().input(), input))
            return ItemStack.EMPTY;
        var product = selected.value().assemble(recipeInput);
        if (product.isEmpty()
                || (!input.has(DataComponents.FOOD) && !product.has(DataComponents.FOOD))) {
            return ItemStack.EMPTY;
        }
        var output = getItem(OUTPUT_SLOT);
        if (!output.isEmpty()
                && (!ItemStack.isSameItemSameComponents(output, product)
                        || output.getCount() + product.getCount() > product.getMaxStackSize()))
            return ItemStack.EMPTY;
        return product;
    }

    public void adjustSpeed(int button) {
        setSpeed(speed + (button == 0 ? 1 : -1));
    }

    public void setSpeed(int requested) {
        int selected = Math.clamp(requested, 0, SPEED_LIMIT);
        if (speed != selected) {
            hbm$syncSet$MicrowaveBlockEntity$speed(selected);
            setChanged();
        }
    }

    @Override
    public long getPower() {
        return energy;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$MicrowaveBlockEntity$energy(Math.clamp(stored, 0L, ENERGY_CAPACITY));
    }

    @Override
    public long getMaxPower() {
        return ENERGY_CAPACITY;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUTPUT_ACCESS : INPUT_ACCESS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != INPUT_SLOT) return false;
        var recipe =
                recipeHint
                        .getRecipeFor(new SingleRecipeInput(stack), (ServerLevel) level)
                        .orElse(null);
        return recipe != null && RecipeInputState.accepts(recipe.value().input(), stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.microwave");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new MicrowaveMenu(id, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$MicrowaveBlockEntity$energy(input.getLongOr("power", 0));
        hbm$syncSet$MicrowaveBlockEntity$speed(input.getIntOr("speed", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", energy);
        output.putInt("speed", speed);
    }

    @Override
    public long syncUnitMask() {
        return 7;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(energy);
            case 1 -> output.writeInt(progress);
            case 2 -> output.writeInt(speed);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$MicrowaveBlockEntity$energy(input.readLong());
            case 1 -> hbm$syncSet$MicrowaveBlockEntity$progress(input.readInt());
            case 2 -> hbm$syncSet$MicrowaveBlockEntity$speed(input.readInt());
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$MicrowaveBlockEntity$energy(long next) {
        if (this.energy != next) {
            this.energy = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$MicrowaveBlockEntity$progress(int next) {
        if (this.progress != next) {
            this.progress = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    public final void hbm$syncSet$MicrowaveBlockEntity$speed(int next) {
        if (this.speed != next) {
            this.speed = next;
            if ((4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4);
        }
    }
}
