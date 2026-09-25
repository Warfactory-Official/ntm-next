// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.IStepTickReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;

public class BlockSpeedySlab extends SlabBlock implements IStepTickReceiver {

    public BlockSpeedySlab(Properties props) {
        super(props);
    }

    @Override
    public void onPlayerStep(Level level, BlockPos pos, Player player) {
        BlockSpeedy.boost(level, player, 1.5D);
    }
}
