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

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Prediction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

public class WeaponTableMenu extends NtmContainerMenu {

    private static final int MOD_SLOTS = 7;
    private static final int WEAPON_SLOT = MOD_SLOTS;
    private static final int PLAYER_START = WEAPON_SLOT + 1;

    private final Inventory playerInventory;
    private final DataSlot configuration = DataSlot.standalone();
    private boolean rebuilding;
    private final SimpleContainer modifications =
            new SimpleContainer(MOD_SLOTS) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    installWorkingModifications();
                }
            };
    private final SimpleContainer workingWeapon =
            new SimpleContainer(1) {
                @Override
                public void setItem(int index, ItemStack stack) {
                    configuration.set(0);
                    super.setItem(index, stack);
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    projectWeaponModifications();
                }
            };

    public WeaponTableMenu(int containerId, Inventory playerInventory) {
        super(ModMenus.WEAPON_TABLE.get(), containerId, null);
        this.playerInventory = playerInventory;

        for (var index = 0; index < MOD_SLOTS; index++) {
            int x = 44 + index * 18;
            addSlot(
                    new SlotFiltered(
                            modifications,
                            index,
                            x,
                            108,
                            stack -> {
                                var weapon = previewStack();
                                return !weapon.isEmpty()
                                        && XWeaponModManager.isApplicable(
                                                weapon, stack, configIndex(), true);
                            }));
        }
        addSlot(
                new SlotFiltered(
                        workingWeapon,
                        0,
                        8,
                        108,
                        stack ->
                                previewStack().isEmpty()
                                        && stack.getItem() instanceof GunBaseNTItem) {
                    @Override
                    public void onTake(Player player, ItemStack stack) {
                        finishConfiguration(stack);
                        super.onTake(player, stack);
                    }
                });
        addStandardInventorySlots(playerInventory, 8, 158);
        addDataSlot(configuration);
    }

    public ItemStack previewStack() {
        return workingWeapon.getItem(0);
    }

    public int configIndex() {
        return configuration.get();
    }

    private ItemStack[] currentModifications() {
        var entries = new ItemStack[MOD_SLOTS];
        for (var index = 0; index < MOD_SLOTS; index++) {
            entries[index] = modifications.getItem(index);
        }
        return entries;
    }

    private void projectWeaponModifications() {
        var weapon = previewStack();
        if (rebuilding || weapon.isEmpty()) {
            return;
        }

        rebuilding = true;
        try {
            var installed = XWeaponModManager.getUpgradeItems(weapon, configIndex());
            int count = Math.min(MOD_SLOTS, installed.length);
            for (var index = 0; index < count; index++) {
                if (installed[index] != null) {
                    modifications.setItem(index, installed[index]);
                }
            }
        } finally {
            rebuilding = false;
        }
    }

    private void installWorkingModifications() {
        var weapon = previewStack();
        if (rebuilding || weapon.isEmpty()) {
            return;
        }
        if (playerInventory.player.level() instanceof ServerLevel level) {
            rebuilding = true;
            try {
                XWeaponModManager.install(level, weapon, configIndex(), currentModifications());
            } finally {
                rebuilding = false;
            }
        }
    }

    private void finishConfiguration(ItemStack weapon) {
        rebuilding = true;
        try {
            if (playerInventory.player.level() instanceof ServerLevel level) {
                XWeaponModManager.install(level, weapon, configIndex(), currentModifications());
            }
            for (var index = 0; index < MOD_SLOTS; index++) {
                var modification = modifications.getItem(index);
                if (!modification.isEmpty()
                        && XWeaponModManager.isApplicable(
                                weapon, modification, configIndex(), false)) {
                    modifications.setItem(index, ItemStack.EMPTY);
                }
            }
        } finally {
            rebuilding = false;
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int target) {
        var weapon = previewStack();
        if (!(weapon.getItem() instanceof GunBaseNTItem gun)
                || target < 0
                || target >= gun.getConfigCount()) {
            return false;
        }
        if (target == configIndex()) {
            return true;
        }

        finishConfiguration(weapon);
        configuration.set(target);
        projectWeaponModifications();
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (player.isSpectator()) {
            return ItemStack.EMPTY;
        }

        var slot = getSlot(slotIndex);
        var source = slot.getItem();
        if (source.isEmpty()) {
            return ItemStack.EMPTY;
        }

        var original = source.copy();
        boolean moved;
        if (slotIndex < PLAYER_START) {
            moved = moveItemStackTo(source, PLAYER_START, slots.size(), true);
        } else if (source.getItem() instanceof GunBaseNTItem) {
            moved = moveItemStackTo(source, WEAPON_SLOT, WEAPON_SLOT + 1, false);
        } else {
            moved = moveItemStackTo(source, 0, MOD_SLOTS, false);
        }
        if (!moved || source.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, original);
        if (source.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level() instanceof ServerLevel level) {
            rebuilding = true;
            try {
                for (var index = 0; index < MOD_SLOTS; index++) {
                    var modification = modifications.removeItemNoUpdate(index);
                    if (!modification.isEmpty()) {
                        player.drop(modification, false, Prediction.SERVER_ONLY);
                    }
                }
                var weapon = workingWeapon.removeItemNoUpdate(0);
                if (!weapon.isEmpty()) {
                    XWeaponModManager.uninstall(level, weapon, configIndex());
                    player.drop(weapon, false, Prediction.SERVER_ONLY);
                }
            } finally {
                rebuilding = false;
            }
        }
    }
}
