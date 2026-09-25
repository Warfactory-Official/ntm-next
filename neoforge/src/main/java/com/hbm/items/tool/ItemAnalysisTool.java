// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.blocks.IAnalyzable;
import com.hbm.blocks.multiblock.MultiblockSurface;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ItemAnalysisTool extends Item {

    public ItemAnalysisTool(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos clicked = ctx.getClickedPos();
        BlockPos target = MultiblockSurface.coreOfAny(level, clicked);
        if (target == null) target = clicked;

        if (!(level.getBlockState(target).getBlock() instanceof IAnalyzable analyzable)) {
            return InteractionResult.PASS;
        }
        List<Component> debug = analyzable.getDebugInfo(level, target);
        Player player = ctx.getPlayer();
        if (debug != null && !level.isClientSide() && player != null) {
            for (Component line : debug)
                player.sendSystemMessage(line.copy().withStyle(ChatFormatting.YELLOW));
        }
        return InteractionResult.SUCCESS;
    }
}
