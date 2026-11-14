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

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.inventory.recipes.PressRecipe;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.inventory.recipes.loader.Outputs;
import com.hbm.items.machine.StampItem;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.util.SyncInterpolation;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public abstract class StampingMachineBlockEntity extends MachineBlockEntity
        implements MenuProvider, SyncUnitSchema {
    public static final int SLOT_STAMP = 1;
    public static final int SLOT_INPUT = 2;
    public static final int SLOT_OUTPUT = 3;
    public static final int MAX_PROGRESS = 200;
    public static final int STROKE_PAUSE_TICKS = 5;

    public int progress;
    public boolean isRetracting;
    public int delay;
    public double renderPress;
    public double lastPress;
    public ItemStack syncStack = ItemStack.EMPTY;

    private final SyncInterpolation strokeEase = new SyncInterpolation();
    private @Nullable List<PressRecipe> catalogue;
    private ItemStack matchedInput = ItemStack.EMPTY;
    private StampItem.@Nullable StampType matchedStamp;
    private @Nullable PressRecipe recipe;
    private @Nullable ItemStack reservation;

    protected StampingMachineBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    public final boolean canProcess() {
        var rows = PressRecipes.INSTANCE.recipes();
        var input = inventory.get(SLOT_INPUT);
        var stamp = StampItem.typeOf(inventory.get(SLOT_STAMP));
        if (rows != catalogue
                || stamp != matchedStamp
                || input.getCount() != matchedInput.getCount()
                || !ItemStack.isSameItemSameComponents(input, matchedInput)) {
            catalogue = rows;
            matchedStamp = stamp;
            matchedInput = input.copy();
            recipe = PressRecipes.INSTANCE.getRecipe(input, stamp);
            reservation = recipe == null ? null : Outputs.maximum(recipe.outputItems()[0]);
        }
        if (recipe == null) return false;
        var stored = inventory.get(SLOT_OUTPUT);
        return stored.isEmpty()
                || reservation != null
                        && (reservation.isEmpty()
                                || ItemStack.isSameItemSameComponents(stored, reservation)
                                        && stored.getCount() + reservation.getCount()
                                                <= stored.getMaxStackSize());
    }

    protected final void completeStamp() {
        syncPress();
        var product = recipe.output();
        inventory.get(SLOT_INPUT).shrink(1);
        wearStamp(inventory, SLOT_STAMP);
        var stored = inventory.get(SLOT_OUTPUT);
        if (stored.isEmpty()) inventory.set(SLOT_OUTPUT, product.copy());
        else stored.grow(product.getCount());
        setChanged();
    }

    protected abstract void syncPress();

    static void wearStamp(List<ItemStack> slots, int slot) {
        var stamp = slots.get(slot);
        if (!stamp.isDamageableItem()) return;
        stamp.setDamageValue(stamp.getDamageValue() + 1);
        if (stamp.getDamageValue() >= stamp.getMaxDamage()) slots.set(slot, ItemStack.EMPTY);
    }

    protected final void writePreview(ByteBuf output) {
        var stack = inventory.get(SLOT_INPUT);
        output.writeInt(stack.isEmpty() ? -1 : Item.getId(stack.getItem()));
        output.writeByte(stack.getCount());
    }

    protected final void readPreview(ByteBuf input) {
        int id = input.readInt();
        int count = input.readByte();
        syncStack = id < 0 ? ItemStack.EMPTY : new ItemStack(Item.byId(id), count);
        inventory.set(SLOT_INPUT, syncStack);
    }

    protected final void readStroke(ByteBuf input) {
        hbm$syncSet$StampingMachineBlockEntity$progress(input.readInt());
        strokeEase.restart(2);
    }

    @Override
    public void tickClient() {
        lastPress = renderPress;
        renderPress = strokeEase.step(renderPress, progress);
    }

    public final RandomSource getRandom() {
        return level.getRandom();
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    public final void hbm$syncSet$StampingMachineBlockEntity$progress(int next) {
        if (this.progress != next) {
            this.progress = next;
            if ((0L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 0L);
        }
    }
}
