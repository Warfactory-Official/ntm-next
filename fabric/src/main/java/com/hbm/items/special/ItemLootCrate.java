// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.items.weapon.ItemCustomMissilePart;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemLootCrate extends Item {

    public static final List<ItemCustomMissilePart> LIST_10 = new ArrayList<>();
    public static final List<ItemCustomMissilePart> LIST_15 = new ArrayList<>();
    public static final List<ItemCustomMissilePart> LIST_MISC = new ArrayList<>();

    private final List<ItemCustomMissilePart> pool;

    public ItemLootCrate(Properties properties, List<ItemCustomMissilePart> pool) {
        super(properties);
        this.pool = pool;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);

        if (level instanceof ServerLevel) {
            player.getInventory()
                    .placeItemBackInInventory(new ItemStack(choose(level.getRandom())));
        }

        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    private ItemCustomMissilePart choose(RandomSource rand) {

        assert !this.pool.isEmpty();

        while (true) {
            ItemCustomMissilePart part = this.pool.get(rand.nextInt(this.pool.size()));

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
