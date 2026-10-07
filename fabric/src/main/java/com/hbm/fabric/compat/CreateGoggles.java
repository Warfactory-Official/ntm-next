// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.compat;

import com.hbm.handler.ArmorModHandler;
import com.zurrtum.create.AllItems;
import com.zurrtum.create.content.equipment.goggles.GogglesItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class CreateGoggles {

    private static final int SOCKETS =
            1 << ArmorModHandler.HELMET_ONLY | 1 << ArmorModHandler.EXTRA;

    private CreateGoggles() {}

    public static void register() {
        ArmorModHandler.registerForeignMod(AllItems.GOGGLES, SOCKETS, EquipmentSlot.HEAD);
        GogglesItem.addIsWearingPredicate(
                player -> {
                    ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
                    return ArmorModHandler.modItem(helmet, ArmorModHandler.HELMET_ONLY)
                                    == AllItems.GOGGLES
                            || ArmorModHandler.modItem(helmet, ArmorModHandler.EXTRA)
                                    == AllItems.GOGGLES;
                });
    }
}
