// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.client.ClientPlayerAccess;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface IPAWeaponsProvider {

    static IPAMelee getMeleeComponentClient() {
        return getMeleeComponentCommon(ClientPlayerAccess.player());
    }

    static IPAMelee getMeleeComponentCommon(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty() && chest.getItem() instanceof IPAWeaponsProvider prov) {
            return prov.getMeleeComponent(player);
        }
        return null;
    }

    static IPARanged getRangedComponentClient() {
        return getRangedComponentCommon(ClientPlayerAccess.player());
    }

    static IPARanged getRangedComponentCommon(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty() && chest.getItem() instanceof IPAWeaponsProvider prov) {
            return prov.getRangedComponent(player);
        }
        return null;
    }

    IPAMelee getMeleeComponent(Player entity);

    IPARanged getRangedComponent(Player entity);
}
