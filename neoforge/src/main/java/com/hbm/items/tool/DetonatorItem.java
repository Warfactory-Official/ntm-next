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

import com.hbm.interfaces.TriggerResult;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import com.hbm.util.KeyHints;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class DetonatorItem extends Item {

    public DetonatorItem(Properties props) {
        super(props);
    }

    private static void message(Player player, Component body) {
        player.sendSystemMessage(
                Component.literal("[")
                        .append(Component.translatable("item.hbm.detonator"))
                        .append("] ")
                        .withStyle(ChatFormatting.DARK_AQUA)
                        .append(body));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Level level = context.getLevel();
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                BlockPos pos = context.getClickedPos();
                context.getItemInHand().set(ModDataComponents.DETONATOR_POS.get(), pos.asLong());
                EntitySounds.play(
                        player,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        ModSounds.TECH_BOOP.get(),
                        SoundSource.PLAYERS,
                        2.0F,
                        1.0F);
                message(
                        player,
                        Component.translatable("item.hbm.detonator.set")
                                .withStyle(ChatFormatting.GREEN));
            }
            return InteractionResult.SUCCESS;
        }
        return detonate(level, player, context.getItemInHand());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return detonate(level, player, player.getItemInHand(hand));
    }

    private InteractionResult detonate(Level level, Player player, ItemStack stack) {
        Long packed = stack.get(ModDataComponents.DETONATOR_POS.get());
        if (packed == null) {
            if (!level.isClientSide())
                message(
                        player,
                        Component.translatable("item.hbm.detonator.nopos")
                                .withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        var target = Detonation.find(level, BlockPos.of(packed));
        if (target == null) {
            if (!level.isClientSide()) message(player, Detonation.report(TriggerResult.NO_DEVICE));
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            EntitySounds.play(
                    player,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.TECH_BLEEP.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
            message(
                    player,
                    Detonation.report(
                            Detonation.fire(
                                    level, target, player, player.getDisplayName().getString())));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(
                Component.translatable(
                        "tooltip.hbm.detonator.set_position",
                        KeyHints.key("key.sneak", "key.keyboard.left.shift"),
                        KeyHints.key("key.use", "key.mouse.right")));
        adder.accept(
                Component.translatable(
                        "tooltip.hbm.detonator.detonate",
                        KeyHints.key("key.use", "key.mouse.right")));
        Long packed = stack.get(ModDataComponents.DETONATOR_POS.get());
        if (packed == null) {
            adder.accept(
                    Component.translatable("item.hbm.detonator.tooltip_nopos")
                            .withStyle(ChatFormatting.RED));
        } else {
            BlockPos pos = BlockPos.of(packed);
            adder.accept(
                    Component.translatable(
                                    "item.hbm.detonator.linked", pos.getX(), pos.getY(), pos.getZ())
                            .withStyle(ChatFormatting.YELLOW));
        }
    }
}
