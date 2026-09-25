// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModItems;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public class BlockForgottenLock extends Block {

    private static final int LENGTH = 15;
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 2);

    public BlockForgottenLock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    private static void generate(Level level, BlockPos pos, Direction side) {
        Direction rot = side.getClockWise();
        BlockState wall = ModBlocks.BRICK_FORGOTTEN.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int w = -2; w <= 2; w++) {
            for (int h = -2; h <= 2; h++) {
                for (int d = 0; d < LENGTH; d++) {
                    BlockState state =
                            Math.abs(w) == 2 || Math.abs(h) == 2 || d == LENGTH - 1 ? wall : air;
                    level.setBlock(
                            pos.offset(
                                    -side.getStepX() * d + rot.getStepX() * w,
                                    h,
                                    -side.getStepZ() * d + rot.getStepZ() * w),
                            state,
                            Block.UPDATE_ALL);
                }
            }
        }
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
        boolean cracked = stack.is(ModItems.KEY_RED_CRACKED.get());
        Direction side = hit.getDirection();
        if ((!cracked && !stack.is(ModItems.KEY_RED.get())) || side.getAxis().isVertical()) {
            return InteractionResult.PASS;
        }
        if (cracked) stack.shrink(1);
        if (level instanceof ServerLevel) {
            generate(level, pos, side);
            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.LOCK_OPEN.get(),
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F);
        }
        return InteractionResult.SUCCESS;
    }
}
