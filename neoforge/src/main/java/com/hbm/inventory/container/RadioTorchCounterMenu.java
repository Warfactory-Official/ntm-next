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

import com.hbm.blockentity.network.RadioCounterBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class RadioTorchCounterMenu extends BlockEntityMenu<RadioCounterBlockEntity> {
    public RadioTorchCounterMenu(
            int containerId, Inventory playerInventory, RadioCounterBlockEntity machine) {
        super(ModMenus.RADIO_TORCH_COUNTER.get(), containerId, machine);

        for (var index = 0; index < RadioCounterBlockEntity.FILTER_COUNT; index++) {
            var y = 18 + index * 44;
            addSlot(new CounterTemplateSlot(machine, index, 138, y));
        }
        addStandardInventorySlots(playerInventory, 12, 156);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput action, Player player) {
        if (slotIndex >= 0 && slotIndex < RadioCounterBlockEntity.FILTER_COUNT) {
            var machine = blockEntity();
            if (action == ContainerInput.PICKUP
                    && button == 1
                    && !machine.getItem(slotIndex).isEmpty()) {
                machine.advanceFilterMode(slotIndex);
            } else {
                machine.setItem(slotIndex, getCarried());
                machine.matcher.initPatternStandard(
                        machine.getLevel(), machine.getItem(slotIndex), slotIndex);
            }
            return;
        }
        super.clicked(slotIndex, button, action, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static final class CounterTemplateSlot extends Slot {
        private CounterTemplateSlot(RadioCounterBlockEntity machine, int index, int x, int y) {
            super(machine, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
