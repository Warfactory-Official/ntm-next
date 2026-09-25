// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.advancement.HbmCriteria;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.sound.ModSounds;
import com.hbm.world.NtmWorldgenFields;
import com.hbm.world.WorldgenHash;
import com.hbm.world.gen.RedRoom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BlockKeyhole extends Block {

    private static final long ROOM = WorldgenHash.identifier(Library.id("red_room"));

    public BlockKeyhole(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(Items.STONE);
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
        return unlock(stack, level, pos, player, hit.getDirection(), false);
    }

    static InteractionResult unlock(
            ItemStack stack,
            Level level,
            BlockPos pos,
            Player player,
            Direction side,
            boolean black) {
        boolean cracked = stack.is(ModItems.KEY_RED_CRACKED.get());
        if ((!cracked && !stack.is(ModItems.KEY_RED.get())) || side.getAxis().isVertical())
            return InteractionResult.PASS;
        if (cracked) stack.shrink(1);
        if (level instanceof ServerLevel server) {
            RedRoom.open(server, pos, side, black, NtmWorldgenFields.get(server).random(ROOM, pos));
            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.LOCK_OPEN.get(),
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F);
            HbmCriteria.ROOM_OPENED.get().trigger((ServerPlayer) player);
        }
        return InteractionResult.SUCCESS;
    }
}
