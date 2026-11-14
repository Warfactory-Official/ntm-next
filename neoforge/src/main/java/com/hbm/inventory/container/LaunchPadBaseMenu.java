/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.inventory.container;

import com.hbm.api.energymk2.EnergyItem;
import com.hbm.api.item.TargetDesignator;
import com.hbm.blockentity.ordnance.launch.LaunchPlatformBlockEntity;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidContainerInfiniteItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public abstract class LaunchPadBaseMenu<T extends LaunchPlatformBlockEntity>
        extends BlockEntityMenu<T> {

    private static final int SLOTS = LaunchPlatformBlockEntity.SLOT_COUNT;

    private final T pad;

    protected LaunchPadBaseMenu(
            MenuType<?> type, int containerId, Inventory playerInv, Container container, T pad) {
        super(type, containerId, pad, container);
        checkContainerSize(container, SLOTS);
        this.pad = pad;

        addSlot(new Slot(container, LaunchPlatformBlockEntity.SLOT_MISSILE, 26, 36));
        addSlot(new Slot(container, LaunchPlatformBlockEntity.SLOT_TARGET, 26, 72));
        addSlot(new Slot(container, LaunchPlatformBlockEntity.SLOT_BATTERY, 107, 90));
        addSlot(new Slot(container, LaunchPlatformBlockEntity.SLOT_FUEL_INPUT, 125, 90));
        addSlot(
                new SlotFiltered(
                        container,
                        LaunchPlatformBlockEntity.SLOT_FUEL_OUTPUT,
                        125,
                        108,
                        SlotFiltered.NONE));
        addSlot(new Slot(container, LaunchPlatformBlockEntity.SLOT_OXIDIZER_INPUT, 143, 90));
        addSlot(
                new SlotFiltered(
                        container,
                        LaunchPlatformBlockEntity.SLOT_OXIDIZER_OUTPUT,
                        143,
                        108,
                        SlotFiltered.NONE));

        addStandardInventorySlots(playerInv, 8, 154);
    }

    private static boolean isBattery(ItemStack stack) {
        return EnergyItem.matches(stack) || stack.is(ModItems.BATTERY_CREATIVE.get());
    }

    private static boolean holds(ItemStack stack, FluidTankNTM tank) {
        Fluid type = tank.getTankType();
        if (type == null) return false;
        if (stack.getItem() instanceof FluidContainerInfiniteItem infinite)
            return infinite.fluid() == type;
        return tank.containerContent(stack) > 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(blockEntity(), player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int invEnd = slots.size();

                    if (index < SLOTS) return moveItemStackTo(stack, SLOTS, invEnd, true);

                    if (isBattery(stack)) {
                        return moveItemStackTo(
                                stack,
                                LaunchPlatformBlockEntity.SLOT_BATTERY,
                                LaunchPlatformBlockEntity.SLOT_BATTERY + 1,
                                false);
                    }
                    if (pad.acceptsMissile(stack)) {
                        return moveItemStackTo(
                                stack,
                                LaunchPlatformBlockEntity.SLOT_MISSILE,
                                LaunchPlatformBlockEntity.SLOT_MISSILE + 1,
                                false);
                    }
                    if (stack.is(ModItems.FLUID_BARREL_INFINITE.get())) {
                        return moveItemStackTo(
                                        stack,
                                        LaunchPlatformBlockEntity.SLOT_FUEL_INPUT,
                                        LaunchPlatformBlockEntity.SLOT_FUEL_INPUT + 1,
                                        false)
                                || moveItemStackTo(
                                        stack,
                                        LaunchPlatformBlockEntity.SLOT_OXIDIZER_INPUT,
                                        LaunchPlatformBlockEntity.SLOT_OXIDIZER_INPUT + 1,
                                        false);
                    }
                    if (holds(stack, pad.fuelTank)) {
                        return moveItemStackTo(
                                stack,
                                LaunchPlatformBlockEntity.SLOT_FUEL_INPUT,
                                LaunchPlatformBlockEntity.SLOT_FUEL_INPUT + 1,
                                false);
                    }
                    if (holds(stack, pad.oxidizerTank)) {
                        return moveItemStackTo(
                                stack,
                                LaunchPlatformBlockEntity.SLOT_OXIDIZER_INPUT,
                                LaunchPlatformBlockEntity.SLOT_OXIDIZER_INPUT + 1,
                                false);
                    }
                    if (stack.getItem() instanceof TargetDesignator) {
                        return moveItemStackTo(
                                stack,
                                LaunchPlatformBlockEntity.SLOT_TARGET,
                                LaunchPlatformBlockEntity.SLOT_TARGET + 1,
                                false);
                    }
                    return false;
                });
    }
}
