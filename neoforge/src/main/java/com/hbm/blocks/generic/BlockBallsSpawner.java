// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.botprime.EntityBOTPrimeHead;
import com.hbm.items.ModItems;
import com.hbm.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BlockBallsSpawner extends Block {

    private static final int DROP_HEIGHT = 300;

    public BlockBallsSpawner(Properties props) {
        super(props);
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
        if (!stack.is(ModItems.MECH_KEY.get())) return InteractionResult.PASS;
        stack.shrink(1);
        if (level instanceof ServerLevel server) {
            EntityBOTPrimeHead bot =
                    ModEntities.BALLS_O_TRON.get().create(server, EntitySpawnReason.TRIGGERED);
            if (bot != null) {
                bot.snapTo(pos.getX() + 0.5, DROP_HEIGHT, pos.getZ() + 0.5, 0, 0);
                bot.setDeltaMovement(0, -1.0, 0);
                Services.PLATFORM.finalizeSpawn(
                        bot,
                        server,
                        server.getCurrentDifficultyAt(bot.blockPosition()),
                        EntitySpawnReason.TRIGGERED,
                        null);
                server.addFreshEntity(bot);
            }
            server.setBlockAndUpdate(pos, ModBlocks.BRICK_JUNGLE_CRACKED.get().defaultBlockState());
        }

        return InteractionResult.PASS;
    }
}
