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
