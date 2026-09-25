// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class BlockForgottenBrick extends Block {
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 6);
    public static final int HOLE = 3;
    public static final int HOLE_EMPTY = 4;

    public BlockForgottenBrick(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(VARIANT) != HOLE || !player.getMainHandItem().isEmpty())
            return InteractionResult.PASS;
        if (!level.isClientSide()) {
            player.setItemInHand(
                    InteractionHand.MAIN_HAND, new ItemStack(ModItems.COAL_ETERNAL.get()));
            level.setBlock(pos, state.setValue(VARIANT, HOLE_EMPTY), Block.UPDATE_ALL);
        }
        return InteractionResult.SUCCESS;
    }
}
