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

package com.hbm.items.food;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public abstract class AppleBaseItem extends Item {

    public final Tier tier;

    protected AppleBaseItem(Properties props, Tier tier) {
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
