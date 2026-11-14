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

import com.hbm.blockentity.machine.ReactorBreedingBlockEntity;
import com.hbm.inventory.slot.SlotRecipeOutput;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ReactorBreedingMenu extends BlockEntityMenu<ReactorBreedingBlockEntity> {

    private final SyncedData data;

    public ReactorBreedingMenu(
            int containerId, Inventory playerInv, ReactorBreedingBlockEntity be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(ReactorBreedingBlockEntity.class)
                        : SyncedData.of(be),
                be);
    }

    private ReactorBreedingMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            ReactorBreedingBlockEntity be) {
        super(ModMenus.MACHINE_REACTOR_BREEDING.get(), containerId, be, container);
        checkContainerSize(container, ReactorBreedingBlockEntity.SLOT_COUNT);
        this.data = data;

        addSlot(new Slot(container, ReactorBreedingBlockEntity.SLOT_INPUT, 35, 35));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        container,
                        ReactorBreedingBlockEntity.SLOT_OUTPUT,
                        125,
                        35));

        addStandardInventorySlots(playerInv, 8, 84);
        addDataSlots(data);
    }

    public int getFlux() {
        return (int) data.get("flux");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveUnsorted(player, index, ReactorBreedingBlockEntity.SLOT_COUNT);
    }
}
