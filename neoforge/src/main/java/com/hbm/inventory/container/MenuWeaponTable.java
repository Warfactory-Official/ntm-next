// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuWeaponTable extends NtmContainerMenu {
    private final Container mods =
            new SimpleContainer(7) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    refreshInstalledMods();
                }
            };
    private final Container gun =
            new SimpleContainer(1) {
                @Override
                public void setItem(int slot, ItemStack stack) {
                    config = 0;
                    super.setItem(slot, stack);
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    loadInstalledMods();
                }
            };
    private final Player player;
    private boolean rebuilding;
    private int config;

    public MenuWeaponTable(int id, Inventory inventory) {
        super(ModMenus.WEAPON_TABLE.get(), id, null);
        player = inventory.player;
        for (int i = 0; i < 7; i++)
            addSlot(
                    new Slot(mods, i, 44 + 18 * i, 108) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            ItemStack held = gun.getItem(0);
                            return !held.isEmpty()
                                    && XWeaponModManager.isApplicable(held, stack, config, true);
                        }
                    });
        addSlot(
                new Slot(gun, 0, 8, 108) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return getItem().isEmpty() && stack.getItem() instanceof ItemGunBaseNT;
                    }

                    @Override
                    public void onTake(Player player, ItemStack stack) {
                        super.onTake(player, stack);
                        finishConfig(stack);
                        config = 0;
                    }
                });
        addStandardInventorySlots(inventory, 8, 158);
        addDataSlot(
                new DataSlot() {
                    @Override
                    public int get() {
                        return config;
                    }

                    @Override
                    public void set(int value) {
                        config = value;
                    }
                });
    }

    public ItemStack previewStack() {
        return gun.getItem(0);
    }

    public int configIndex() {
        return config;
    }

    private void loadInstalledMods() {
        if (rebuilding || gun.getItem(0).isEmpty()) return;
        rebuilding = true;
        ItemStack[] installed = XWeaponModManager.getUpgradeItems(gun.getItem(0), config);
        for (int i = 0; i < Math.min(installed.length, mods.getContainerSize()); i++)
            if (installed[i] != null) mods.setItem(i, installed[i]);
        rebuilding = false;
    }

    private void refreshInstalledMods() {
        if (rebuilding
                || gun.getItem(0).isEmpty()
                || !(player.level() instanceof ServerLevel level)) return;
        XWeaponModManager.install(
                level,
                gun.getItem(0),
                config,
                mods.getItem(0),
                mods.getItem(1),
                mods.getItem(2),
                mods.getItem(3),
                mods.getItem(4),
                mods.getItem(5),
                mods.getItem(6));
    }

    private void finishConfig(ItemStack stack) {
        if (!(player.level() instanceof ServerLevel level)) return;
        XWeaponModManager.install(
                level,
                stack,
                config,
                mods.getItem(0),
                mods.getItem(1),
                mods.getItem(2),
                mods.getItem(3),
                mods.getItem(4),
                mods.getItem(5),
                mods.getItem(6));
        rebuilding = true;
        for (int i = 0; i < mods.getContainerSize(); i++) {
            ItemStack mod = mods.getItem(i);
            if (XWeaponModManager.isApplicable(stack, mod, config, false))
                mods.setItem(i, ItemStack.EMPTY);
        }
        rebuilding = false;
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        ItemStack stack = gun.getItem(0);
        if (!(stack.getItem() instanceof ItemGunBaseNT weapon)
                || button < 0
                || button >= weapon.getConfigCount()) return false;
        if (button == config) return true;
        finishConfig(stack);
        config = button;
        loadInstalledMods();
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < 8) {
            if (!moveItemStackTo(stack, 8, slots.size(), true)) return ItemStack.EMPTY;
            slot.onTake(player, copy);
        } else if (stack.getItem() instanceof ItemGunBaseNT) {
            if (!moveItemStackTo(stack, 7, 8, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 7, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return stack.getCount() == copy.getCount() ? ItemStack.EMPTY : copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!(player.level() instanceof ServerLevel level)) return;
        for (int i = 0; i < mods.getContainerSize(); i++) {
            ItemStack mod = mods.removeItemNoUpdate(i);
            if (!mod.isEmpty()) player.drop(mod, false);
        }
        ItemStack stack = gun.removeItemNoUpdate(0);
        if (!stack.isEmpty()) {
            XWeaponModManager.uninstall(level, stack, config);
            player.drop(stack, false);
        }
    }
}
