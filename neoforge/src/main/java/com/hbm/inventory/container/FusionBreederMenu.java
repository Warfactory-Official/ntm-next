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

import com.hbm.blockentity.machine.fusion.FusionNeutronBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class FusionBreederMenu extends BlockEntityMenu<FusionNeutronBlockEntity> {

    public FusionBreederMenu(int containerId, Inventory playerInv, FusionNeutronBlockEntity be) {
        super(ModMenus.FUSION_BREEDER.get(), containerId, be);
        checkContainerSize(be, FusionNeutronBlockEntity.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        be,
                        FusionNeutronBlockEntity.SLOT_FLUID_ID,
                        26,
                        72,
                        stack -> stack.getItem() instanceof FluidIdentifierItem));
        addSlot(
                new SlotFiltered(
                        be,
                        FusionNeutronBlockEntity.SLOT_INPUT,
                        48,
                        45,
                        stack ->
                                be.canPlaceItem(FusionNeutronBlockEntity.SLOT_INPUT, stack)
                                        || stack.is(ModItems.METEORITE_SWORD_IRRADIATED.get())));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, be, FusionNeutronBlockEntity.SLOT_OUTPUT, 112, 45));

        addStandardInventorySlots(playerInv, 8, 118);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = FusionNeutronBlockEntity.SLOT_COUNT;

                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                FusionNeutronBlockEntity.SLOT_FLUID_ID,
                                FusionNeutronBlockEntity.SLOT_FLUID_ID + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            FusionNeutronBlockEntity.SLOT_INPUT,
                            FusionNeutronBlockEntity.SLOT_INPUT + 1,
                            false);
                });
    }
}
