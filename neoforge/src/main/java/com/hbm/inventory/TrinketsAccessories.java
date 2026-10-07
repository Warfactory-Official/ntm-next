// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import eu.pb4.trinkets.api.TrinketsApi;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

public final class TrinketsAccessories {
    private TrinketsAccessories() {}

    public static void collect(Player player, List<Container> out) {
        out.addAll(TrinketsApi.getAttachment(player).getInventories().values());
    }
}
