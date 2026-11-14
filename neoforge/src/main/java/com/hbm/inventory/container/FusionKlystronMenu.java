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
import com.hbm.blockentity.machine.fusion.FusionKlystronBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class FusionKlystronMenu extends BlockEntityMenu<FusionKlystronBlockEntity> {

    private static final int SLOT_COUNT = 1;

    public FusionKlystronMenu(int containerId, Inventory playerInv, FusionKlystronBlockEntity be) {
        super(ModMenus.FUSION_KLYSTRON.get(), containerId, be);
        checkContainerSize(be, SLOT_COUNT);

        addSlot(new SlotFiltered(be, 0, 8, 72, EnergyItem::matches));

        addStandardInventorySlots(playerInv, 17, 118);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index < SLOT_COUNT)
                        return moveItemStackTo(stack, SLOT_COUNT, slots.size(), true);
                    if (EnergyItem.matches(stack)) return moveItemStackTo(stack, 0, 1, false);
                    return false;
                });
    }
}
