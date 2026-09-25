// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.interfaces.IItemEntityUpdate;
import com.hbm.items.ModItems;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemRag extends ItemCustomLore implements IItemEntityUpdate {

    public ItemRag(Properties properties) {
        super(properties);
    }

    @Override
    public boolean updateDroppedItem(ItemEntity entity) {
        if (!entity.level().getFluidState(entity.blockPosition()).is(FluidTags.WATER)) return false;

        entity.setItem(new ItemStack(ModItems.RAG_DAMP.get(), entity.getItem().getCount()));
        return true;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.RAG_PISS));
            player.getItemInHand(hand).shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
