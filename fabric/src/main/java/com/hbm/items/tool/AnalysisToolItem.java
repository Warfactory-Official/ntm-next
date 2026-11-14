/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.items.tool;

import com.hbm.blocks.DebugInfoProvider;
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

public class AnalysisToolItem extends Item {

    public AnalysisToolItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos clicked = ctx.getClickedPos();
        BlockPos target = MultiblockSurface.coreOfAny(level, clicked);
        if (target == null) target = clicked;

        if (!(level.getBlockState(target).getBlock() instanceof DebugInfoProvider analyzable)) {
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
