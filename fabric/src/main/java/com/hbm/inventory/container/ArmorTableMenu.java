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

import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.slot.SlotFiltered;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ArmorTableMenu extends NtmContainerMenu {

    private static final int ARMOR_SLOT = ArmorModHandler.MOD_SLOTS;
    private static final int EQUIPMENT_START = ARMOR_SLOT + 1;
    private static final int EQUIPMENT_END = EQUIPMENT_START + 4;
    private static final int PLAYER_START = EQUIPMENT_END;

    private boolean rebuilding;
    private final SimpleContainer modifications =
            new SimpleContainer(ArmorModHandler.MOD_SLOTS) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    updateArmorComponents();
                }
            };
    private final SimpleContainer workingArmor =
            new SimpleContainer(1) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    projectArmorComponents();
                }
            };

    public ArmorTableMenu(int containerId, Inventory playerInventory) {
        super(ModMenus.ARMOR_TABLE.get(), containerId, null);

        addModificationSlot(ArmorModHandler.HELMET_ONLY, 48, 27);
        addModificationSlot(ArmorModHandler.PLATE_ONLY, 84, 27);
        addModificationSlot(ArmorModHandler.LEGS_ONLY, 120, 27);
        addModificationSlot(ArmorModHandler.BOOTS_ONLY, 156, 45);
        addModificationSlot(ArmorModHandler.SERVOS, 156, 81);
        addModificationSlot(ArmorModHandler.CLADDING, 120, 99);
        addModificationSlot(ArmorModHandler.KEVLAR, 84, 99);
        addModificationSlot(ArmorModHandler.EXTRA, 48, 99);
        addModificationSlot(ArmorModHandler.BATTERY, 30, 63);
        addSlot(
                new SlotFiltered(
                        workingArmor,
                        0,
                        66,
                        63,
                        stack -> armorStack().isEmpty() && ArmorModHandler.isArmor(stack)) {
                    @Override
                    public void onTake(Player player, ItemStack stack) {
                        clearProjection();
                        super.onTake(player, stack);
                    }
                });

        addEquipmentSlot(playerInventory, EquipmentSlot.HEAD, 0);
        addEquipmentSlot(playerInventory, EquipmentSlot.CHEST, 1);
        addEquipmentSlot(playerInventory, EquipmentSlot.LEGS, 2);
        addEquipmentSlot(playerInventory, EquipmentSlot.FEET, 3);
        addStandardInventorySlots(playerInventory, 30, 140);
    }

    private void addModificationSlot(int socket, int x, int y) {
        addSlot(
                new SlotFiltered(
                        modifications,
                        socket,
                        x,
                        y,
                        stack -> {
                            var armor = armorStack();
                            return !armor.isEmpty()
                                    && ArmorModHandler.fits(stack, socket)
                                    && ArmorModHandler.isApplicable(armor, stack);
                        }));
    }

    private void addEquipmentSlot(Inventory playerInventory, EquipmentSlot equipmentSlot, int row) {
        int inventoryIndex = equipmentSlot.getIndex(Inventory.INVENTORY_SIZE);
        int y = 36 + row * 18;
        addSlot(
                new Slot(playerInventory, inventoryIndex, 5, y) {
                    @Override
                    public int getMaxStackSize() {
                        return 1;
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return playerInventory.player.isEquippableInSlot(stack, equipmentSlot);
                    }
                });
    }

    public ItemStack armorStack() {
        return workingArmor.getItem(0);
    }

    private void projectArmorComponents() {
        var armor = armorStack();
        if (rebuilding || armor.isEmpty()) {
            return;
        }

        rebuilding = true;
        try {
            var installed = ArmorModHandler.pryMods(armor);
            for (var socket = 0; socket < ArmorModHandler.MOD_SLOTS; socket++) {
                modifications.setItem(socket, installed[socket]);
            }
        } finally {
            rebuilding = false;
        }
    }

    private void updateArmorComponents() {
        var armor = armorStack();
        if (rebuilding || armor.isEmpty()) {
            return;
        }

        for (var socket = 0; socket < ArmorModHandler.MOD_SLOTS; socket++) {
            var modification = modifications.getItem(socket);
            if (modification.isEmpty()) {
                ArmorModHandler.removeMod(armor, socket);
            } else if (ArmorModHandler.fits(modification, socket)
                    && ArmorModHandler.isApplicable(armor, modification)) {
                ArmorModHandler.applyMod(armor, modification, socket);
            }
        }
    }

    private void clearProjection() {
        rebuilding = true;
        try {
            modifications.clearContent();
        } finally {
            rebuilding = false;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return quickMove(
                player,
                slotIndex,
                false,
                stack -> {
                    if (slotIndex == ARMOR_SLOT) {
                        boolean moved =
                                moveItemStackTo(stack, EQUIPMENT_START, EQUIPMENT_END, false);
                        if (!stack.isEmpty()) {
                            moved |= moveItemStackTo(stack, PLAYER_START, slots.size(), true);
                        }
                        return moved;
                    }
                    if (slotIndex < PLAYER_START) {
                        return moveItemStackTo(stack, PLAYER_START, slots.size(), true);
                    }
                    if (ArmorModHandler.isArmor(stack)) {
                        return moveItemStackTo(stack, ARMOR_SLOT, ARMOR_SLOT + 1, false);
                    }
                    for (var socket = 0; socket < ArmorModHandler.MOD_SLOTS; socket++) {
                        if (modifications.getItem(socket).isEmpty()
                                && moveItemStackTo(stack, socket, socket + 1, false)) {
                            return true;
                        }
                    }
                    return false;
                });
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level() instanceof ServerLevel) {
            rebuilding = true;
            try {
                var armor = armorStack();
                for (var socket = 0; socket < ArmorModHandler.MOD_SLOTS; socket++) {
                    var modification = modifications.removeItemNoUpdate(socket);
                    if (!modification.isEmpty()) {
                        player.drop(modification, false);
                        if (!armor.isEmpty()) {
                            ArmorModHandler.removeMod(armor, socket);
                        }
                    }
                }
                var detachedArmor = workingArmor.removeItemNoUpdate(0);
                if (!detachedArmor.isEmpty()) {
                    player.drop(detachedArmor, false);
                }
            } finally {
                rebuilding = false;
            }
        }
    }
}
