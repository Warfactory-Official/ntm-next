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

package com.hbm.blockentity.network.pneumatic;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.ItemData;
import com.hbm.inventory.container.PneumoStorageClutterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueOutput;

public final class PneumaticStorageClutterBlockEntity extends PneumaticStorageBankBlockEntity {

    public static final int SLOT_COUNT = 54;

    public PneumaticStorageClutterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PNEUMATIC_STORAGE_CLUTTER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.pneumoStorageClutter");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new PneumoStorageClutterMenu(containerId, playerInventory, this);
    }

    @Override
    public long quantityAt(int slot) {
        return getSlotAt(slot).getCount();
    }

    @Override
    public boolean canAssignTypes() {
        return true;
    }

    @Override
    public long removeFromSlot(int slot, long amount) {
        int removed =
                removeItem(slot, (int) Math.min(amount, getSlotAt(slot).getCount())).getCount();
        return amount - removed;
    }

    @Override
    public long fillExistingSlot(int slot, long amount) {
        var stack = getSlotAt(slot);
        if (stack.isEmpty()) return amount;
        int room = getMaxStackSize(stack) - stack.getCount();
        if (room <= 0) return amount;
        int accepted = (int) Math.min(amount, room);
        stack.grow(accepted);
        setChanged();
        return amount - accepted;
    }

    @Override
    public long assignSlotType(int slot, ItemStack type, long amount) {
        int accepted = (int) Math.min(amount, getMaxStackSize(type));
        setItem(slot, type.copyWithCount(accepted));
        return amount - accepted;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (!ItemData.CRATE_KEEP_CONTENTS.get()) super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (ItemData.CRATE_KEEP_CONTENTS.get()) {
            components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(inventory));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        components
                .getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .copyInto(inventory);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("inventory");
    }
}
