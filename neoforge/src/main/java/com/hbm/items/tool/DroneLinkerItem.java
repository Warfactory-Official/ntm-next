/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.items.tool;

import com.hbm.blockentity.network.DroneContainer;
import com.hbm.items.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class DroneLinkerItem extends Item {

    public DroneLinkerItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player != null && player.isSpectator()) return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof DroneContainer clicked))
            return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ItemStack stack = context.getItemInHand();
        Long stored = stack.get(ModDataComponents.DRONE_LINK_TARGET.get());

        if (stored == null) {
            say(player, stack, "Set initial position!", ChatFormatting.AQUA);
        } else if (level.getBlockEntity(BlockPos.of(stored)) instanceof DroneContainer previous) {
            previous.linkNext(clicked.hoverPoint());
            say(player, stack, "Link set!", ChatFormatting.AQUA);
        } else {
            say(player, stack, "Previous link lost!", ChatFormatting.RED);
        }

        stack.set(ModDataComponents.DRONE_LINK_TARGET.get(), pos.asLong());
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isSpectator()) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !stack.has(ModDataComponents.DRONE_LINK_TARGET.get())) {
            return InteractionResult.PASS;
        }

        stack.remove(ModDataComponents.DRONE_LINK_TARGET.get());
        say(player, stack, "Position cleared!", ChatFormatting.GREEN);
        return InteractionResult.SUCCESS;
    }

    private static void say(
            @Nullable Player player, ItemStack stack, String message, ChatFormatting colour) {
        if (player == null) return;
        player.sendSystemMessage(
                Component.literal("[")
                        .withStyle(ChatFormatting.DARK_AQUA)
                        .append(stack.getHoverName().copy().withStyle(ChatFormatting.DARK_AQUA))
                        .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA))
                        .append(Component.literal(message).withStyle(colour)));
    }
}
