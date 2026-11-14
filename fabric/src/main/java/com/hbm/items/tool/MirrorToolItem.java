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

import com.hbm.blockentity.machine.SolarMirrorBlockEntity;
import com.hbm.blocks.machine.SolarBoilerBlock;
import com.hbm.blocks.machine.SolarMirrorBlock;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.items.ModDataComponents;
import com.hbm.util.TooltipStyle;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class MirrorToolItem extends Item {

    public MirrorToolItem(Properties props) {
        super(props);
    }

    private static void message(Player player, String key, ChatFormatting color) {
        if (player != null) player.sendSystemMessage(Component.translatable(key).withStyle(color));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Block b = level.getBlockState(pos).getBlock();
        ItemStack stack = ctx.getItemInHand();
        Player player = ctx.getPlayer();
        if (player != null && player.isSpectator()) return InteractionResult.PASS;

        BlockPos boilerCore = MultiblockSurface.coreOfAny(level, pos);
        if (boilerCore != null
                && level.getBlockState(boilerCore).getBlock() instanceof SolarBoilerBlock) {
            if (!level.isClientSide()) {
                stack.set(ModDataComponents.MIRROR_TOOL_TARGET.get(), boilerCore.above().asLong());
                message(player, "item.hbm.mirror_tool.linked", ChatFormatting.YELLOW);
            }
            return InteractionResult.SUCCESS;
        }

        if (b instanceof SolarMirrorBlock) {
            Long target = stack.get(ModDataComponents.MIRROR_TOOL_TARGET.get());
            if (target == null) return InteractionResult.PASS;
            if (!level.isClientSide()
                    && level.getBlockEntity(pos) instanceof SolarMirrorBlockEntity mirror) {
                BlockPos t = BlockPos.of(target);
                double dx = (double) pos.getX() - t.getX();
                double dy = (double) pos.getY() - t.getY();
                double dz = (double) pos.getZ() - t.getZ();

                boolean withinReach = dx * dx + dy * dy + dz * dz <= 10000;
                boolean withinAngle = dx * dx + dz * dz <= dy * dy;

                if (!withinReach) message(player, "item.hbm.mirror_tool.reach", ChatFormatting.RED);
                else if (!withinAngle)
                    message(player, "item.hbm.mirror_tool.angle", ChatFormatting.RED);
                else mirror.setTarget(t.getX(), t.getY(), t.getZ());
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        for (Component line :
                TooltipStyle.lines(
                        Component.translatableWithFallback(getDescriptionId() + ".desc", ""))) {
            adder.accept(line.copy().withStyle(ChatFormatting.YELLOW));
        }
    }
}
