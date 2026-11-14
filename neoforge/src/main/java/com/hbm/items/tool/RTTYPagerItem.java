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

import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.SmallExplosionEffect;
import com.hbm.explosion.vanillant.standard.WeaponExplosionEntityProcessor;
import com.hbm.handler.radio.RadioChannel;
import com.hbm.handler.radio.RadioSystem;
import com.hbm.inventory.container.HeldItemMenu;
import com.hbm.inventory.container.ModMenus;
import com.hbm.items.ModDataComponents;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.PlayerInformLayout;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class RTTYPagerItem extends Item {

    public static final String KEY_CHANNEL = "chan";
    public static final int ID_PAGER_DYN = 1_000;
    public static final int MAX_CHANNEL_LENGTH = 15;

    public RTTYPagerItem(Properties properties) {
        super(properties);
    }

    public static String channel(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.RTTY_PAGER_CHANNEL.get(), "");
    }

    public static void setChannel(ItemStack stack, String channel) {
        stack.set(ModDataComponents.RTTY_PAGER_CHANNEL.get(), channel);
    }

    public static void tickInventory(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            List<ItemStack> inventory = player.getInventory().getNonEquipmentItems();
            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack stack = inventory.get(slot);
                if (stack.getItem() instanceof RTTYPagerItem) tickStack(stack, player, slot);
            }
        }
    }

    private static void tickStack(ItemStack stack, ServerPlayer player, int slot) {
        if (!stack.has(ModDataComponents.RTTY_PAGER_CHANNEL.get())) return;

        ServerLevel level = player.level();
        String channel = channel(stack);
        RadioChannel receiver = RadioSystem.read(level, channel);
        if (receiver == null || receiver.stamp() < level.getGameTime() - 1) return;

        if ("selfdestruct".equals(receiver.text())) {
            if (player.isSpectator()) return;
            ComposableExplosion explosion =
                    new ComposableExplosion(
                            level,
                            player.getX(),
                            player.getY() + player.getBbHeight() / 2,
                            player.getZ(),
                            5,
                            null);
            explosion.entityProcessor(new WeaponExplosionEntityProcessor(1, 50).piercing(5F, 0.5F));
            explosion.effects(new SmallExplosionEffect(10, 2.5F, 1F));
            explosion.explode();
            stack.shrink(1);
            return;
        }

        int alive = player.tickCount % 1_000;
        String message =
                ChatFormatting.GOLD
                        + "[ "
                        + channel
                        + " ("
                        + alive
                        + ") ] "
                        + ChatFormatting.YELLOW
                        + receiver.text();
        PacketWire.sendTo(
                PlayerInformLayout.encode(Component.literal(message), ID_PAGER_DYN + slot, 5_000),
                player);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        HeldItemMenu.open(
                player, hand, ModMenus.RTTY_PAGER, Component.translatable("container.rttyPager"));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        if (!stack.has(ModDataComponents.RTTY_PAGER_CHANNEL.get()) || channel(stack).isEmpty()) {
            adder.accept(
                    Component.translatable("desc.item.rttyPager.noChannelSet")
                            .withStyle(ChatFormatting.RED));
        } else {
            adder.accept(
                    Component.translatable("desc.item.rttyPager.channel", channel(stack))
                            .withStyle(ChatFormatting.YELLOW));
        }
    }
}
