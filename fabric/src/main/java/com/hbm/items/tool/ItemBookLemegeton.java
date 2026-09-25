// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.inventory.container.MenuLemegeton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class ItemBookLemegeton extends Item {

    public ItemBookLemegeton(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide())
            player.openMenu(
                    new SimpleMenuProvider(
                            (id, inventory, owner) -> new MenuLemegeton(id, inventory),
                            Component.translatable(getDescriptionId())));
        return InteractionResult.SUCCESS;
    }
}
