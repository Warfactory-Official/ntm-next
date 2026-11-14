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
import com.hbm.blockentity.machine.albion.PAQuadrupoleBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.PACoilItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PAQuadrupoleMenu extends BlockEntityMenu<PAQuadrupoleBlockEntity> {

    public PAQuadrupoleMenu(int containerId, Inventory playerInv, PAQuadrupoleBlockEntity be) {
        super(ModMenus.PA_QUADRUPOLE.get(), containerId, be);
        checkContainerSize(be, PAQuadrupoleBlockEntity.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        container(),
                        PAQuadrupoleBlockEntity.SLOT_BATTERY,
                        26,
                        72,
                        EnergyItem::matches));
        addSlot(
                new SlotFiltered(
                        container(),
                        PAQuadrupoleBlockEntity.SLOT_COIL,
                        71,
                        36,
                        stack -> stack.getItem() instanceof PACoilItem));

        addStandardInventorySlots(playerInv, 8, 122);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = PAQuadrupoleBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                PAQuadrupoleBlockEntity.SLOT_BATTERY,
                                PAQuadrupoleBlockEntity.SLOT_BATTERY + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            PAQuadrupoleBlockEntity.SLOT_COIL,
                            PAQuadrupoleBlockEntity.SLOT_COIL + 1,
                            false);
                });
    }
}
