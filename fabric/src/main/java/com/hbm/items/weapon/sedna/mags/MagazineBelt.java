/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.items.weapon.sedna.mags;

import com.hbm.inventory.ItemStackContainer;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.tool.AmmoBagItem;
import com.hbm.items.tool.CasingBagItem;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.mags.Magazine;
import com.hbm.particle.casing.Casing;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MagazineBelt implements Magazine<BulletConfig> {
    private static final int INFINITE_ROUNDS = 9_999;

    public static final String KEY_MAG_TYPE = "magtype";
    public List<BulletConfig> acceptedBullets = new ArrayList<>();

    public static int getMagType(ItemStack stack) {
        return GunBaseNTItem.getValueInt(stack, KEY_MAG_TYPE);
    }

    public static void setMagType(ItemStack stack, int value) {
        GunBaseNTItem.setValueInt(stack, KEY_MAG_TYPE, value);
    }

    public MagazineBelt addConfigs(BulletConfig... cfgs) {
        Collections.addAll(acceptedBullets, cfgs);
        return this;
    }

    @Override
    public BulletConfig getType(ItemStack stack, Container inventory) {
        BulletConfig config = getFirstConfig(stack, inventory);
        if (getMagType(stack) != config.id) {
            setMagType(stack, config.id);
        }
        return config;
    }

    @Override
    public void consumeAmmo(ItemStack stack, Container inventory, int amount) {
        if (inventory == null) return;
        if (!ArmorSuitEffects.spendsRound(inventory)) return;

        BulletConfig first = this.getFirstConfig(stack, inventory);

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);

            if (!slot.isEmpty()) {
                if (first.matchesAmmo(slot)) {
                    int toRemove = Math.min(slot.getCount(), amount);
                    amount -= toRemove;
                    inventory.removeItem(i, toRemove);
                    CasingBagItem.collectCasings(inventory, first, toRemove);
                    if (amount <= 0) return;
                }

                ItemStackContainer bag = AmmoBagItem.heldContents(slot);
                if (bag != null) {
                    boolean infinite = AmmoBagItem.isInfinite(slot);
                    for (int j = 0; j < bag.getContainerSize(); j++) {
                        ItemStack inBag = bag.getItem(j);
                        if (inBag.isEmpty() || !first.matchesAmmo(inBag)) continue;
                        int toRemove = Math.min(inBag.getCount(), amount);
                        amount -= toRemove;
                        if (!infinite) bag.removeItem(j, toRemove);
                        CasingBagItem.collectCasings(inventory, first, toRemove);
                        if (amount <= 0) return;
                    }
                }
            }
        }
    }

    @Override
    public void setType(ItemStack stack, BulletConfig type) {}

    @Override
    public int getCapacity(ItemStack stack) {
        return 0;
    }

    @Override
    public void setAmount(ItemStack stack, int amount) {}

    @Override
    public boolean canReload(ItemStack stack, Container inventory) {
        return false;
    }

    @Override
    public void initNewType(ItemStack stack, Container inventory) {}

    @Override
    public void reloadAction(ItemStack stack, Container inventory) {}

    @Override
    public void setAmountBeforeReload(ItemStack stack, int amount) {}

    @Override
    public int getAmountBeforeReload(ItemStack stack) {
        return 0;
    }

    @Override
    public void setAmountAfterReload(ItemStack stack, int amount) {}

    @Override
    public int getAmountAfterReload(ItemStack stack) {
        return 0;
    }

    @Override
    public int getAmount(ItemStack stack, Container inventory) {
        if (inventory == null) return 1;
        BulletConfig first = this.getFirstConfig(stack, inventory);
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);

            if (!slot.isEmpty()) {
                if (first.matchesAmmo(slot)) count += slot.getCount();

                ItemStackContainer bag = AmmoBagItem.heldContents(slot);
                if (bag != null) {
                    boolean infinite = AmmoBagItem.isInfinite(slot);
                    for (int j = 0; j < bag.getContainerSize(); j++) {
                        ItemStack inBag = bag.getItem(j);
                        if (inBag.isEmpty() || !first.matchesAmmo(inBag)) continue;
                        if (infinite) return INFINITE_ROUNDS;
                        count += inBag.getCount();
                    }
                }
            }
        }
        return count;
    }

    @Override
    public ItemStack getIconForHud(ItemStack stack, Player player) {
        BulletConfig first = this.getFirstConfig(stack, player.getInventory());
        return first.ammoStack();
    }

    @Override
    public String getAmmoStateForHud(ItemStack stack, Player player) {
        return "x" + getAmount(stack, player.getInventory());
    }

    @Override
    public Casing getCasing(ItemStack stack, Container invnetory) {
        return getFirstConfig(stack, invnetory).casing;
    }

    public BulletConfig getFirstConfig(ItemStack stack, Container inventory) {

        if (inventory == null) return acceptedBullets.get(0);

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);

            if (!slot.isEmpty()) {
                for (BulletConfig config : this.acceptedBullets) {
                    if (config.matchesAmmo(slot)) return config;
                }

                ItemStackContainer bag = AmmoBagItem.heldContents(slot);
                if (bag != null) {
                    for (int j = 0; j < bag.getContainerSize(); j++) {
                        ItemStack inBag = bag.getItem(j);
                        if (inBag.isEmpty()) continue;
                        for (BulletConfig config : this.acceptedBullets) {
                            if (config.matchesAmmo(inBag)) return config;
                        }
                    }
                }
            }
        }

        BulletConfig cached = BulletConfig.configs.get(getMagType(stack));
        return acceptedBullets.contains(cached) ? cached : acceptedBullets.get(0);
    }
}
