// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.tileentity.network.BlockEntityPylonBase;
import com.hbm.util.ColorUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public abstract class PylonBlockBase extends CableConductorBlockBase implements EntityBlock {

    protected PylonBlockBase(Properties props) {
        super(props);
    }

    public static InteractionResult dyeAt(
            Level level, BlockPos pos, ItemStack stack, Player player) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityPylonBase pylon))
            return InteractionResult.PASS;
        int dyed = ColorUtil.getColorFromDye(stack);
        if (dyed == 0 || dyed == pylon.color) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        pylon.setColor(dyed, stack, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        return dyeAt(level, pos, stack, player);
    }
}
