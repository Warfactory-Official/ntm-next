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

package com.hbm.items.special;

import com.hbm.items.weapon.CustomMissilePartItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class LootCrateItem extends Item {

    public static final List<CustomMissilePartItem> LIST_10 = new ArrayList<>();
    public static final List<CustomMissilePartItem> LIST_15 = new ArrayList<>();
    public static final List<CustomMissilePartItem> LIST_MISC = new ArrayList<>();

    private final List<CustomMissilePartItem> pool;

    public LootCrateItem(Properties properties, List<CustomMissilePartItem> pool) {
        super(properties);
        this.pool = pool;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);

        if (level instanceof ServerLevel) {
            player.getInventory()
                    .placeItemBackInInventory(
                            new ItemStack(choose(level.getRandom())), Prediction.SERVER_ONLY);
        }

        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    private CustomMissilePartItem choose(RandomSource rand) {

        assert !this.pool.isEmpty();

        while (true) {
            CustomMissilePartItem part = this.pool.get(rand.nextInt(this.pool.size()));

            int odds =
                    switch (part.rarity) {
                        case COMMON -> 1;
                        case UNCOMMON -> 5;
                        case RARE -> 10;
                        case EPIC -> 25;
                        case LEGENDARY -> 50;
                        case STRANGE -> 100;
                    };

            if (rand.nextInt(odds) == 0) return part;
        }
    }
}
