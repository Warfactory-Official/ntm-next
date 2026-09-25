// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.entity.mob.EntityQuackos;
import com.hbm.items.special.ItemCustomLore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemPeas extends ItemCustomLore {

    private static final double RANGE = 50D;

    public ItemPeas(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        stack.consume(1, player);
        if (level instanceof ServerLevel server) {
            for (EntityQuackos duck :
                    server.getEntitiesOfClass(
                            EntityQuackos.class, player.getBoundingBox().inflate(RANGE))) {
                duck.despawn();
            }
        }
        return InteractionResult.CONSUME;
    }
}
