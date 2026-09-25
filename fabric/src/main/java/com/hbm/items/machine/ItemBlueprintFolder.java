// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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

public class ItemBlueprintFolder extends Item {

    public final Kind kind;

    public ItemBlueprintFolder(Properties properties, Kind kind) {
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
                ItemBlueprints.make(pools.get(player.getRandom().nextInt(pools.size())));
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
