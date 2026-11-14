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

import com.hbm.api.energymk2.EnergyItem;
import com.hbm.blockentity.machine.fusion.FusionTorusBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.BlueprintsItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class FusionTorusMenu extends BlockEntityMenu<FusionTorusBlockEntity> {

    public FusionTorusMenu(int containerId, Inventory playerInv, FusionTorusBlockEntity be) {
        super(ModMenus.FUSION_TORUS.get(), containerId, be);
        checkContainerSize(be, FusionTorusBlockEntity.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        be, FusionTorusBlockEntity.SLOT_BATTERY, 8, 82, EnergyItem::matches));
        addSlot(
                new SlotFiltered(
                        be,
                        FusionTorusBlockEntity.SLOT_BLUEPRINT,
                        71,
                        81,
                        stack -> stack.getItem() instanceof BlueprintsItem));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, be, FusionTorusBlockEntity.SLOT_OUTPUT, 130, 36));

        addStandardInventorySlots(playerInv, 35, 162);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = FusionTorusBlockEntity.SLOT_COUNT;
                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                FusionTorusBlockEntity.SLOT_BATTERY,
                                FusionTorusBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof BlueprintsItem)
                        return moveItemStackTo(
                                stack,
                                FusionTorusBlockEntity.SLOT_BLUEPRINT,
                                FusionTorusBlockEntity.SLOT_BLUEPRINT + 1,
                                false);
                    return false;
                });
    }
}
