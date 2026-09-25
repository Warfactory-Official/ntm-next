// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BlockWriting extends Block {

    private static final String[] MESSAGE = {
        "chat.blockWriting.youShouldNotHave",
        "chat.blockWriting.thisIsNotA",
        "chat.blockWriting.nothingOfValueIs",
        "chat.blockWriting.whatIsHereIs",
        "chat.blockWriting.weConsideredOurselvesA",
        "chat.blockWriting.thenWeSawThe",
        "chat.blockWriting.andWeWereAfraid",
        "chat.blockWriting.weBuiltGreatTombs",
        "chat.blockWriting.ifThisPlaceIs",
        "chat.blockWriting.leaveThisPlaceAnd"
    };

    public BlockWriting(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            for (String line : MESSAGE) {
                player.sendSystemMessage(
                        Component.translatable(line).withStyle(ChatFormatting.RED));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
