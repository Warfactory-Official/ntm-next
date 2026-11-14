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

import com.hbm.blockentity.machine.MissileWorkbenchBlockEntity;
import com.hbm.inventory.slot.SlotRecipeOutput;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MissileAssemblyMenu extends BlockEntityMenu<MissileWorkbenchBlockEntity> {

    public MissileAssemblyMenu(
            int containerId, Inventory playerInv, MissileWorkbenchBlockEntity be) {
        super(ModMenus.MACHINE_MISSILE_ASSEMBLY.get(), containerId, be, be);
        checkContainerSize(be, MissileWorkbenchBlockEntity.INVENTORY_SIZE);

        addSlot(new Slot(be, MissileWorkbenchBlockEntity.GUIDANCE_SLOT, 8, 36));
        addSlot(new Slot(be, MissileWorkbenchBlockEntity.PAYLOAD_SLOT, 26, 36));
        addSlot(new Slot(be, MissileWorkbenchBlockEntity.BODY_SLOT, 44, 36));
        addSlot(new Slot(be, MissileWorkbenchBlockEntity.STABILIZER_SLOT, 62, 36));
        addSlot(new Slot(be, MissileWorkbenchBlockEntity.PROPULSION_SLOT, 80, 36));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, be, MissileWorkbenchBlockEntity.PRODUCT_SLOT, 152, 36));

        addStandardInventorySlots(playerInv, 8, 140);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack ->
                        index < MissileWorkbenchBlockEntity.INVENTORY_SIZE
                                ? moveItemStackTo(
                                        stack,
                                        MissileWorkbenchBlockEntity.INVENTORY_SIZE,
                                        slots.size(),
                                        true)
                                : moveItemStackTo(
                                        stack, 0, MissileWorkbenchBlockEntity.PRODUCT_SLOT, false));
    }
}
