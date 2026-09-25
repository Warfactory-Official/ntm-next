// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public abstract class ItemAppleBase extends Item {

    public final Tier tier;

    protected ItemAppleBase(Properties props, Tier tier) {
        super(props.rarity(tier.rarity));
        this.tier = tier;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return tier == Tier.BLOCK || super.isFoil(stack);
    }

    public enum Tier {
        NUGGET(Rarity.UNCOMMON),
        INGOT(Rarity.RARE),
        BLOCK(Rarity.EPIC);

        public final Rarity rarity;

        Tier(Rarity rarity) {
            this.rarity = rarity;
        }
    }
}
