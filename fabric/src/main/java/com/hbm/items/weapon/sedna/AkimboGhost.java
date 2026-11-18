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

package com.hbm.items.weapon.sedna;

import com.hbm.items.ModDataComponents;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class AkimboGhost {

    private AkimboGhost() {}

    public static boolean isAkimbo(ItemStack stack) {
        return !isGhost(stack) && isAkimbo(stack.getItem());
    }

    public static boolean isAkimbo(Item item) {
        return item instanceof GunBaseNTItem gun && gun.getConfigCount() > 1;
    }

    public static boolean isGhost(ItemStack stack) {
        return stack.has(ModDataComponents.AKIMBO_GHOST.get());
    }

    public static void tick(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        sweep(player, inventory);

        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);

        if (isAkimbo(main)) {
            if (!off.isEmpty() && !isGhost(off)) {
                player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                giveBack(player, off);
                off = ItemStack.EMPTY;
            }
            ItemStack ghost = ghostOf(main);
            if (!ItemStack.matches(off, ghost))
                player.setItemInHand(InteractionHand.OFF_HAND, ghost);
            return;
        }

        if (isGhost(off)) {
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        } else if (isAkimbo(off)) {
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            if (main.isEmpty()) player.setItemInHand(InteractionHand.MAIN_HAND, off);
            else giveBack(player, off);
        }
    }

    private static void sweep(ServerPlayer player, Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (slot == Inventory.SLOT_OFFHAND) continue;
            if (isGhost(inventory.getItem(slot))) inventory.setItem(slot, ItemStack.EMPTY);
        }
        if (isGhost(player.containerMenu.getCarried()))
            player.containerMenu.setCarried(ItemStack.EMPTY);
    }

    private static ItemStack ghostOf(ItemStack main) {
        ItemStack ghost = main.copyWithCount(1);
        ghost.set(ModDataComponents.AKIMBO_GHOST.get(), true);
        return ghost;
    }

    private static void giveBack(ServerPlayer player, ItemStack stack) {
        player.getInventory().placeItemBackInInventory(stack, true, Prediction.SERVER_ONLY);
    }
}
