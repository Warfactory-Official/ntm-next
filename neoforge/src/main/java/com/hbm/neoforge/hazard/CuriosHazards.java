// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.hazard;

import com.hbm.hazard.HazardSystem;
import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public final class CuriosHazards {
    private CuriosHazards() {}

    public static float apply(Player player) {
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) return 0F;
        float activation = 0F;
        for (ICurioStacksHandler slots : inventory.getCurios().values()) {
            activation += apply(slots.getStacks(), player);
            activation += apply(slots.getCosmeticStacks(), player);
        }
        return activation;
    }

    private static float apply(IDynamicStackHandler slots, Player player) {
        float activation = 0F;
        for (int slot = 0; slot < slots.getSlots(); slot++) {
            activation += HazardSystem.applyExternalEquipment(slots.getStackInSlot(slot), player);
        }
        return activation;
    }
}
