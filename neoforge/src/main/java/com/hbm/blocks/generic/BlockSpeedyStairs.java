// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.IStepTickReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BlockSpeedyStairs extends StairBlock implements IStepTickReceiver {

    private final double speed;

    public BlockSpeedyStairs(BlockState baseState, double speed, Properties props) {
        super(baseState, props);
        this.speed = speed;
    }

    @Override
    public void onPlayerStep(Level level, BlockPos pos, Player player) {
        BlockSpeedy.boost(level, player, speed);
    }
}
