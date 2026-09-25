// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.mags;

import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.ItemStackContainer;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.items.tool.ItemAmmoBag;
import com.hbm.items.tool.ItemCasingBag;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.particle.SpentCasing;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public interface IMagazine<T> {

    static void handleAmmoBag(Container inventory, BulletConfig config, int shotsFired) {
        if (config.casingItem == null || config.casingAmount <= 0) return;
        if (!(inventory instanceof Inventory playerInv)) return;

        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = playerInv.getItem(slot);
            if (!(stack.getItem() instanceof ItemCasingBag)) continue;
            float share = 1F / config.casingAmount * 0.5F * shotsFired;
            if (ItemCasingBag.pushCasing(stack, config.casingItem.get(), share)) return;
        }
    }

    static @Nullable ItemStackContainer ammoBag(ItemStack slot) {
        return slot.getItem() instanceof ItemAmmoBag ? ItemAmmoBag.contents(slot) : null;
    }

    static boolean isInfiniteBag(ItemStack slot) {
        return slot.getItem() instanceof ItemAmmoBag bag && bag.infinite();
    }

    static boolean shouldUseUpTrenchie(Container inv) {
        if (inv instanceof Inventory playerInv
                && (ArmorSuitEffects.hasFullSet(playerInv.player, ModArmorItem.Suit.TRENCHMASTER)
                        || ArmorModHandler.pryMod(
                                        playerInv.player.getItemBySlot(EquipmentSlot.HEAD),
                                        ArmorModHandler.HELMET_ONLY)
                                .is(ModItems.CARD_AOS.get()))) {
            return playerInv.player.getRandom().nextInt(3) < 2;
        }
        return true;
    }

    T getType(ItemStack stack, Container inventory);

    void setType(ItemStack stack, T type);

    int getCapacity(ItemStack stack);

    int getAmount(ItemStack stack, Container inventory);

    void setAmount(ItemStack stack, int amount);

    void useUpAmmo(ItemStack stack, Container inventory, int amount);

    boolean canReload(ItemStack stack, Container inventory);

    void initNewType(ItemStack stack, Container inventory);

    void reloadAction(ItemStack stack, Container inventory);

    ItemStack getIconForHUD(ItemStack stack, Player player);

    String reportAmmoStateForHUD(ItemStack stack, Player player);

    SpentCasing getCasing(ItemStack stack, Container inventory);

    void setAmountBeforeReload(ItemStack stack, int amount);

    int getAmountBeforeReload(ItemStack stack);

    void setAmountAfterReload(ItemStack stack, int amount);

    int getAmountAfterReload(ItemStack stack);
}
