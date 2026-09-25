// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.mods;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public interface IWeaponMod {

    int getModPriority();

    String[] getSlots();

    <T> T eval(T base, ItemStack gun, String key, Object parent);

    default void onInstall(ItemStack gun, ItemStack mod, int index) {}

    default void onUninstall(ItemStack gun, ItemStack mod, int index) {}

    default void onInstall(ServerLevel level, ItemStack gun, ItemStack mod, int index) {
        onInstall(gun, mod, index);
    }

    default void onUninstall(ServerLevel level, ItemStack gun, ItemStack mod, int index) {
        onUninstall(gun, mod, index);
    }
}
