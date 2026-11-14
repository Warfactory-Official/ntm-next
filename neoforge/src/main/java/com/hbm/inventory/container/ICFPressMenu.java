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

import com.hbm.blockentity.machine.FusionPelletPressBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ICFPressMenu extends BlockEntityMenu<FusionPelletPressBlockEntity> {

    public ICFPressMenu(int containerId, Inventory playerInv, FusionPelletPressBlockEntity be) {
        this(containerId, playerInv, be, be);
    }

    private ICFPressMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            FusionPelletPressBlockEntity be) {
        super(ModMenus.MACHINE_ICF_PRESS.get(), containerId, be, container);
        checkContainerSize(container, FusionPelletPressBlockEntity.INVENTORY_SIZE);

        addSlot(new Slot(container, FusionPelletPressBlockEntity.SHELL_SLOT, 98, 18));
        addSlot(
                new SlotFiltered(
                        container,
                        FusionPelletPressBlockEntity.PELLET_SLOT,
                        98,
                        54,
                        SlotFiltered.NONE));
        addSlot(new Slot(container, FusionPelletPressBlockEntity.MUON_SLOT, 8, 18));
        addSlot(
                new SlotFiltered(
                        container,
                        FusionPelletPressBlockEntity.REMAINDER_SLOT,
                        8,
                        54,
                        SlotFiltered.NONE));
        addSlot(new Slot(container, FusionPelletPressBlockEntity.SOLID_A_SLOT, 62, 54));
        addSlot(new Slot(container, FusionPelletPressBlockEntity.SOLID_B_SLOT, 134, 54));
        addSlot(new Slot(container, FusionPelletPressBlockEntity.IDENTIFIER_A_SLOT, 62, 18));
        addSlot(new Slot(container, FusionPelletPressBlockEntity.IDENTIFIER_B_SLOT, 134, 18));

        addStandardInventorySlots(playerInv, 8, 97);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        int machineEnd = FusionPelletPressBlockEntity.INVENTORY_SIZE;
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (stack.getItem() instanceof FluidIdentifierItem) {
                        return moveItemStackTo(
                                stack,
                                FusionPelletPressBlockEntity.IDENTIFIER_A_SLOT,
                                FusionPelletPressBlockEntity.IDENTIFIER_B_SLOT + 1,
                                false);
                    }
                    if (stack.is(ModItems.ICF_PELLET_EMPTY.get())) {
                        return moveItemStackTo(
                                stack,
                                FusionPelletPressBlockEntity.SHELL_SLOT,
                                FusionPelletPressBlockEntity.SHELL_SLOT + 1,
                                false);
                    }
                    if (stack.is(ModItems.PARTICLE_MUON.get())) {
                        return moveItemStackTo(
                                stack,
                                FusionPelletPressBlockEntity.MUON_SLOT,
                                FusionPelletPressBlockEntity.MUON_SLOT + 1,
                                false);
                    }
                    return moveItemStackTo(
                            stack,
                            FusionPelletPressBlockEntity.SOLID_A_SLOT,
                            FusionPelletPressBlockEntity.SOLID_B_SLOT + 1,
                            false);
                });
    }
}
