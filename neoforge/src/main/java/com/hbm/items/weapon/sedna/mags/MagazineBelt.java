// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.mags;

import com.hbm.inventory.ItemStackContainer;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.particle.SpentCasing;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MagazineBelt implements IMagazine<BulletConfig> {

    private static final int INFINITE_ROUNDS = 9_999;

    public static final String KEY_MAG_TYPE = "magtype";
    public List<BulletConfig> acceptedBullets = new ArrayList<>();

    public static int getMagType(ItemStack stack) {
        return ItemGunBaseNT.getValueInt(stack, KEY_MAG_TYPE);
    }

    public static void setMagType(ItemStack stack, int value) {
        ItemGunBaseNT.setValueInt(stack, KEY_MAG_TYPE, value);
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
    public void useUpAmmo(ItemStack stack, Container inventory, int amount) {
        if (inventory == null) return;
        if (!IMagazine.shouldUseUpTrenchie(inventory)) return;

        BulletConfig first = this.getFirstConfig(stack, inventory);

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);

            if (!slot.isEmpty()) {
                if (first.matchesAmmo(slot)) {
                    int toRemove = Math.min(slot.getCount(), amount);
                    amount -= toRemove;
                    inventory.removeItem(i, toRemove);
                    IMagazine.handleAmmoBag(inventory, first, toRemove);
                    if (amount <= 0) return;
                }

                ItemStackContainer bag = IMagazine.ammoBag(slot);
                if (bag != null) {
                    boolean infinite = IMagazine.isInfiniteBag(slot);
                    for (int j = 0; j < bag.getContainerSize(); j++) {
                        ItemStack inBag = bag.getItem(j);
                        if (inBag.isEmpty() || !first.matchesAmmo(inBag)) continue;
                        int toRemove = Math.min(inBag.getCount(), amount);
                        amount -= toRemove;
                        if (!infinite) bag.removeItem(j, toRemove);
                        IMagazine.handleAmmoBag(inventory, first, toRemove);
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

                ItemStackContainer bag = IMagazine.ammoBag(slot);
                if (bag != null) {
                    boolean infinite = IMagazine.isInfiniteBag(slot);
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
    public ItemStack getIconForHUD(ItemStack stack, Player player) {
        BulletConfig first = this.getFirstConfig(stack, player.getInventory());
        return first.ammoStack();
    }

    @Override
    public String reportAmmoStateForHUD(ItemStack stack, Player player) {
        return "x" + getAmount(stack, player.getInventory());
    }

    @Override
    public SpentCasing getCasing(ItemStack stack, Container invnetory) {
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

                ItemStackContainer bag = IMagazine.ammoBag(slot);
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
