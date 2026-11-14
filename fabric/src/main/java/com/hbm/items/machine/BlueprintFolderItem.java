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

package com.hbm.items.machine;

import com.hbm.inventory.recipes.loader.GenericRecipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BlueprintFolderItem extends Item {

    public final Kind kind;

    public BlueprintFolderItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ItemStack folder = player.getItemInHand(hand);
        String prefix = kind.poolPrefix;
        List<String> pools = new ArrayList<>();
        for (String pool : GenericRecipes.pools().keySet()) {
            if (pool.startsWith(prefix)) pools.add(pool);
        }
        if (pools.isEmpty()) return InteractionResult.PASS;

        ItemStack blueprint =
                BlueprintsItem.make(pools.get(player.getRandom().nextInt(pools.size())));
        player.getInventory().placeItemBackInInventory(blueprint);
        folder.shrink(1);
        return InteractionResult.SUCCESS;
    }

    public enum Kind {
        ALT(GenericRecipes.POOL_PREFIX_ALT),
        DISCOVER(GenericRecipes.POOL_PREFIX_DISCOVER),
        SECRET(GenericRecipes.POOL_PREFIX_SECRET);

        public final String poolPrefix;

        Kind(String poolPrefix) {
            this.poolPrefix = poolPrefix;
        }
    }
}
