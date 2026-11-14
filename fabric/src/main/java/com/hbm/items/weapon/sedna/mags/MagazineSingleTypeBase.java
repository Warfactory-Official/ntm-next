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
import com.hbm.util.NtmMath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public abstract class MagazineSingleTypeBase implements Magazine<BulletConfig> {
    private static final int INFINITE_ROUNDS = 9_999;

    public static final String KEY_MAG_COUNT = "magcount";
    public static final String KEY_MAG_TYPE = "magtype";
    public static final String KEY_MAG_PREV = "magprev";
    public static final String KEY_MAG_AFTER = "magafter";

    public List<BulletConfig> acceptedBullets = new ArrayList<>();

    public int index;
    public int capacity;

    public MagazineSingleTypeBase(int index, int capacity) {
        this.index = index;
        this.capacity = capacity;
    }

    public static int getMagType(ItemStack stack, int index) {
        return GunBaseNTItem.getValueInt(stack, KEY_MAG_TYPE + index);
    }

    public static void setMagType(ItemStack stack, int index, int value) {
        GunBaseNTItem.setValueInt(stack, KEY_MAG_TYPE + index, value);
    }

    public static int getMagCount(ItemStack stack, int index) {
        return GunBaseNTItem.getValueInt(stack, KEY_MAG_COUNT + index);
    }

    public static void setMagCount(ItemStack stack, int index, int value) {
        GunBaseNTItem.setValueInt(stack, KEY_MAG_COUNT + index, value);
    }

    public MagazineSingleTypeBase addConfigs(BulletConfig... cfgs) {
        Collections.addAll(acceptedBullets, cfgs);
        return this;
    }

    @Override
    public BulletConfig getType(ItemStack stack, Container inventory) {
        int type = getMagType(stack, index);
        if (type >= 0 && type < BulletConfig.configs.size()) {
            BulletConfig cfg = BulletConfig.configs.get(type);
            if (acceptedBullets.contains(cfg)) return cfg;
            return acceptedBullets.get(0);
        }
        return null;
    }

    @Override
    public void setType(ItemStack stack, BulletConfig type) {
        int i = BulletConfig.configs.indexOf(type);
        if (i >= 0) setMagType(stack, index, i);
    }

    @Override
    public ItemStack getIconForHud(ItemStack stack, Player player) {
        BulletConfig config = this.getType(stack, player.getInventory());
        if (config != null) return config.ammoStack();
        return null;
    }

    @Override
    public String getAmmoStateForHud(ItemStack stack, Player player) {
        return getAmount(stack, player.getInventory()) + " / " + getCapacity(stack);
    }

    @Override
    public Casing getCasing(ItemStack stack, Container inventory) {
        return this.getType(stack, inventory).casing;
    }

    @Override
    public void consumeAmmo(ItemStack stack, Container inventory, int amount) {
        if (!ArmorSuitEffects.spendsRound(inventory) && getCapacity(stack) != 1) return;
        this.setAmount(stack, this.getAmount(stack, inventory) - amount);
        CasingBagItem.collectCasings(inventory, this.getType(stack, inventory), amount);
    }

    @Override
    public boolean canReload(ItemStack stack, Container inventory) {
        if (this.getAmount(stack, inventory) >= this.getCapacity(stack)) return false;
        if (inventory == null) return true;
        BulletConfig nextConfig = getFirstConfig(stack, inventory);
        return nextConfig != null;
    }

    public void standardReload(ItemStack stack, Container inventory, int loadLimit) {

        if (inventory == null) {
            BulletConfig config = this.getType(stack, inventory);
            if (config == null) {
                config = this.acceptedBullets.get(0);
                this.setType(stack, config);
            }
            this.setAmount(stack, this.capacity);
            return;
        }

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);

            if (loadLimit <= 0) return;

            if (!slot.isEmpty()) {
                if (this.getAmount(stack, null) == 0) {

                    for (BulletConfig config : this.acceptedBullets) {
                        if (config.matchesAmmo(slot)) {
                            this.setType(stack, config);
                            int wantsToLoad =
                                    (int)
                                            Math.ceil(
                                                    (double) this.getCapacity(stack)
                                                            / (double) config.ammoReloadCount);
                            int toLoad = NtmMath.min(wantsToLoad, slot.getCount(), loadLimit);
                            this.setAmount(
                                    stack,
                                    Math.min(toLoad * config.ammoReloadCount, this.capacity));
                            inventory.removeItem(i, toLoad);
                            loadLimit -= toLoad;
                            break;
                        }
                    }
                } else {
                    BulletConfig config = this.getType(stack, null);
                    if (config == null) {
                        config = this.acceptedBullets.get(0);
                        this.setType(stack, config);
                    }

                    if (config.matchesAmmo(slot)) {
                        int alreadyLoaded = this.getAmount(stack, null);
                        int wantsToLoad =
                                (int)
                                        Math.ceil(
                                                (double) (this.getCapacity(stack) - alreadyLoaded)
                                                        / (double) config.ammoReloadCount);
                        int toLoad = NtmMath.min(wantsToLoad, slot.getCount(), loadLimit);
                        this.setAmount(
                                stack,
                                Math.min(
                                        (toLoad * config.ammoReloadCount) + alreadyLoaded,
                                        this.capacity));
                        inventory.removeItem(i, toLoad);
                        loadLimit -= toLoad;
                    }
                }

                ItemStackContainer bag = AmmoBagItem.heldContents(slot);
                if (bag != null) {
                    boolean infinite = AmmoBagItem.isInfinite(slot);
                    for (int j = 0; j < bag.getContainerSize(); j++) {
                        ItemStack inBag = bag.getItem(j);
                        if (inBag.isEmpty()) continue;
                        int available = infinite ? INFINITE_ROUNDS : inBag.getCount();

                        if (this.getAmount(stack, null) == 0) {
                            for (BulletConfig config : this.acceptedBullets) {
                                if (!config.matchesAmmo(inBag)) continue;
                                this.setType(stack, config);
                                int wantsToLoad =
                                        (int)
                                                Math.ceil(
                                                        (double) this.getCapacity(stack)
                                                                / (double) config.ammoReloadCount);
                                int toLoad = NtmMath.min(wantsToLoad, available, loadLimit);
                                this.setAmount(
                                        stack,
                                        Math.min(toLoad * config.ammoReloadCount, this.capacity));
                                if (!infinite) bag.removeItem(j, toLoad);
                                loadLimit -= toLoad;
                                break;
                            }
                        } else {
                            BulletConfig config = this.getType(stack, null);
                            if (config == null) {
                                config = this.acceptedBullets.get(0);
                                this.setType(stack, config);
                            }
                            if (!config.matchesAmmo(inBag)) continue;
                            int alreadyLoaded = this.getAmount(stack, bag);
                            int wantsToLoad =
                                    (int)
                                            Math.ceil(
                                                    (double)
                                                                    (this.getCapacity(stack)
                                                                            - alreadyLoaded)
                                                            / (double) config.ammoReloadCount);
                            int toLoad = NtmMath.min(wantsToLoad, available, loadLimit);
                            this.setAmount(
                                    stack,
                                    Math.min(
                                            (toLoad * config.ammoReloadCount) + alreadyLoaded,
                                            this.capacity));
                            if (!infinite) bag.removeItem(j, toLoad);
                            loadLimit -= toLoad;
                        }
                    }
                }
            }
        }
    }

    public BulletConfig getFirstConfig(ItemStack stack, Container inventory) {
        if (inventory == null) return null;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);

            if (!slot.isEmpty()) {
                if (this.getAmount(stack, null) == 0) {
                    for (BulletConfig config : this.acceptedBullets) {
                        if (config.matchesAmmo(slot)) return config;
                    }
                } else {
                    BulletConfig config = this.getType(stack, null);
                    if (config == null) {
                        config = this.acceptedBullets.get(0);
                        this.setType(stack, config);
                    }
                    if (config.matchesAmmo(slot)) return config;
                }

                ItemStackContainer bag = AmmoBagItem.heldContents(slot);
                if (bag != null) {
                    for (int j = 0; j < bag.getContainerSize(); j++) {
                        ItemStack inBag = bag.getItem(j);
                        if (inBag.isEmpty()) continue;
                        if (this.getAmount(stack, null) == 0) {
                            for (BulletConfig config : this.acceptedBullets) {
                                if (config.matchesAmmo(inBag)) return config;
                            }
                        } else {
                            BulletConfig config = this.getType(stack, null);
                            if (config == null) {
                                config = this.acceptedBullets.get(0);
                                this.setType(stack, config);
                            }
                            if (config.matchesAmmo(inBag)) return config;
                        }
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void initNewType(ItemStack stack, Container inventory) {
        if (inventory == null) return;
        BulletConfig nextConfig = getFirstConfig(stack, inventory);
        if (nextConfig != null) {
            int i = BulletConfig.configs.indexOf(nextConfig);
            setMagType(stack, index, i);
        }
    }

    @Override
    public int getCapacity(ItemStack stack) {
        return capacity;
    }

    @Override
    public int getAmount(ItemStack stack, Container inventory) {
        return getMagCount(stack, index);
    }

    @Override
    public void setAmount(ItemStack stack, int amount) {
        setMagCount(stack, index, Math.max(amount, 0));
    }

    @Override
    public void setAmountBeforeReload(ItemStack stack, int amount) {
        GunBaseNTItem.setValueInt(stack, KEY_MAG_PREV + index, amount);
    }

    @Override
    public int getAmountBeforeReload(ItemStack stack) {
        return GunBaseNTItem.getValueInt(stack, KEY_MAG_PREV + index);
    }

    @Override
    public void setAmountAfterReload(ItemStack stack, int amount) {
        GunBaseNTItem.setValueInt(stack, KEY_MAG_AFTER + index, amount);
    }

    @Override
    public int getAmountAfterReload(ItemStack stack) {
        return GunBaseNTItem.getValueInt(stack, KEY_MAG_AFTER + index);
    }
}
