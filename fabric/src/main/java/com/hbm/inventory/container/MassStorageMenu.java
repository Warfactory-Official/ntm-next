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

package com.hbm.inventory.container;

import com.hbm.blockentity.machine.storage.MassStorageBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotPattern;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MassStorageMenu extends BlockEntityMenu<MassStorageBlockEntity> {
    private final boolean opensContainer;

    public MassStorageMenu(int containerId, Inventory playerInv, MassStorageBlockEntity storage) {
        this(containerId, playerInv, storage, storage);
    }

    private MassStorageMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            MassStorageBlockEntity storage) {
        super(ModMenus.MASS_STORAGE.get(), containerId, storage, container);
        checkContainerSize(container, MassStorageBlockEntity.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        storage,
                        MassStorageBlockEntity.SLOT_INPUT,
                        61,
                        17,
                        stack -> storage.canPlaceItem(MassStorageBlockEntity.SLOT_INPUT, stack)));
        addSlot(new SlotPattern(container, MassStorageBlockEntity.SLOT_TYPE, 61, 53));
        addSlot(
                new SlotFiltered(
                        storage, MassStorageBlockEntity.SLOT_OUTPUT, 61, 89, SlotFiltered.NONE));

        addStandardInventorySlots(playerInv, 8, 139);
        opensContainer = !playerInv.player.isSpectator();
        if (opensContainer) container.startOpen(playerInv.player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (opensContainer) container().stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        MassStorageBlockEntity storage = blockEntity();
        int machineEnd = MassStorageBlockEntity.SLOT_COUNT;

        if (index == MassStorageBlockEntity.SLOT_OUTPUT && !getSlot(index).hasItem()) {
            getSlot(index).set(storage.quickExtract());
        }

        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (storage.quickInsert(stack)) return true;
                    return moveItemStackTo(
                            stack,
                            MassStorageBlockEntity.SLOT_INPUT,
                            MassStorageBlockEntity.SLOT_INPUT + 1,
                            false);
                });
    }

    @Override
    public void clicked(
            int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        if (slotIndex != MassStorageBlockEntity.SLOT_TYPE) {
            super.clicked(slotIndex, buttonNum, containerInput, player);
            return;
        }
        if (blockEntity().getStockpile() > 0 || !blockEntity().admitsFilter(getCarried())) return;
        getSlot(slotIndex).set(getCarried());
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.index != MassStorageBlockEntity.SLOT_TYPE
                && super.canTakeItemForPickAll(stack, slot);
    }
}
