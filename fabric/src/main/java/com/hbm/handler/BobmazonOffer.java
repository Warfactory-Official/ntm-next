// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import net.minecraft.world.item.ItemStack;

public record BobmazonOffer(
        ItemStack offer,
        BobmazonRequirement requirement,
        int cost,
        int stars,
        String comment,
        String author) {

    public static final String NO_RATINGS = "No Ratings";

    public BobmazonOffer(ItemStack offer, BobmazonRequirement requirement, int cost) {
        this(offer, requirement, cost, 0);
    }

    public BobmazonOffer(ItemStack offer, BobmazonRequirement requirement, int cost, int stars) {
        this(offer, requirement, cost, stars, NO_RATINGS, "");
    }

    public int barWidth() {
        return stars * 4 - 1;
    }
}
