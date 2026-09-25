// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.items.tool.ItemToolAbility;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlockDepth extends Block {

    public BlockDepth(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected float getDestroyProgress(
            BlockState state, Player player, BlockGetter level, BlockPos pos) {
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof ItemToolAbility tool && tool.canBreakDepthRock(held))
            return 1.0F / 50.0F;
        return super.getDestroyProgress(state, player, level, pos);
    }
}
