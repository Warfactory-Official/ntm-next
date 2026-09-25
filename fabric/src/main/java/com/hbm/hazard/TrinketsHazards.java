// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard;

import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.world.entity.player.Player;

public final class TrinketsHazards {
    private TrinketsHazards() {}

    public static float apply(Player player) {
        float activation = 0F;
        for (TrinketInventory inventory :
                TrinketsApi.getAttachment(player).getInventories().values()) {
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                activation += HazardSystem.applyExternalEquipment(inventory.getItem(slot), player);
                if (inventory.hasCosmeticItems()) {
                    activation +=
                            HazardSystem.applyExternalEquipment(
                                    inventory.getCosmeticItem(slot), player);
                }
            }
        }
        return activation;
    }
}
