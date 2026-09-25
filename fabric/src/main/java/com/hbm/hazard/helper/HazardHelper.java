// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard.helper;

import com.hbm.items.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public final class HazardHelper {

    private HazardHelper() {}

    public static boolean isHoldingReacher(LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            if (inventory.getItem(i).is(ModItems.REACHER.get())) return true;
        }
        return false;
    }
}
