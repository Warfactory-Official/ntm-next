// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna;

import com.hbm.items.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
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
        return item instanceof ItemGunBaseNT gun && gun.getConfigCount() > 1;
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
        player.getInventory().placeItemBackInInventory(stack, true);
    }
}
