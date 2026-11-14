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

import com.hbm.blockentity.machine.dfc.DfcInjectorBlockEntity;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CoreInjectorMenu extends BlockEntityMenu<DfcInjectorBlockEntity> {

    public CoreInjectorMenu(int containerId, Inventory playerInv, DfcInjectorBlockEntity be) {
        super(ModMenus.CORE_INJECTOR.get(), containerId, be);
        checkContainerSize(be, DfcInjectorBlockEntity.SLOT_COUNT);

        addSlot(new Slot(be, DfcInjectorBlockEntity.SLOT_FILL_A, 26, 17));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, be, DfcInjectorBlockEntity.SLOT_DRAIN_A, 26, 53));
        addSlot(new Slot(be, DfcInjectorBlockEntity.SLOT_FILL_B, 134, 17));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, be, DfcInjectorBlockEntity.SLOT_DRAIN_B, 134, 53));

        addStandardInventorySlots(playerInv, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    if (index < DfcInjectorBlockEntity.SLOT_COUNT)
                        return moveItemStackTo(
                                stack, DfcInjectorBlockEntity.SLOT_COUNT, slots.size(), true);
                    if (stack.getItem() instanceof FluidIdentifierItem) {
                        if (moveItemStackTo(
                                stack,
                                DfcInjectorBlockEntity.SLOT_FILL_A,
                                DfcInjectorBlockEntity.SLOT_FILL_A + 1,
                                false)) return true;
                        return moveItemStackTo(
                                stack,
                                DfcInjectorBlockEntity.SLOT_FILL_B,
                                DfcInjectorBlockEntity.SLOT_FILL_B + 1,
                                false);
                    }
                    return false;
                });
    }
}
