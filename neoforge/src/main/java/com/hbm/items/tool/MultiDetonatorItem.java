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

import com.hbm.items.ModDataComponents;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import com.hbm.util.KeyHints;
import java.util.ArrayList;
import java.util.List;
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

public class MultiDetonatorItem extends Item {

    public MultiDetonatorItem(Properties props) {
        super(props);
    }

    private static List<Long> positions(ItemStack stack) {
        List<Long> stored = stack.get(ModDataComponents.DETONATOR_POS_LIST.get());
        return stored == null ? List.of() : stored;
    }

    private static void message(Player player, Component body) {
        player.sendSystemMessage(
                Component.literal("[")
                        .append(Component.translatable("item.hbm.detonator_multi"))
                        .append("] ")
                        .withStyle(ChatFormatting.DARK_AQUA)
                        .append(body));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;

        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        List<Long> grown = new ArrayList<>(positions(stack));
        grown.add(context.getClickedPos().asLong());
        stack.set(ModDataComponents.DETONATOR_POS_LIST.get(), List.copyOf(grown));

        EntitySounds.play(
                player,
                player.blockPosition(),
                ModSounds.TECH_BOOP.get(),
                SoundSource.PLAYERS,
                2.0F,
                1.0F);
        if (!level.isClientSide()) {
            message(
                    player,
                    Component.translatable("item.hbm.detonator_multi.added")
                            .withStyle(ChatFormatting.GREEN));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        List<Long> linked = positions(stack);

        if (linked.isEmpty()) {
            if (!level.isClientSide()) {
                message(
                        player,
                        Component.translatable("item.hbm.detonator_multi.nopos")
                                .withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }

        if (player.isShiftKeyDown()) {
            stack.remove(ModDataComponents.DETONATOR_POS_LIST.get());
            EntitySounds.play(
                    player,
                    player.blockPosition(),
                    ModSounds.TECH_BOOP.get(),
                    SoundSource.PLAYERS,
                    2.0F,
                    1.0F);
            if (!level.isClientSide()) {
                message(
                        player,
                        Component.translatable("item.hbm.detonator_multi.cleared")
                                .withStyle(ChatFormatting.RED));
            }
            return InteractionResult.SUCCESS;
        }

        int fired = 0;
        if (!level.isClientSide()) {
            for (long packed : linked) {
                var target = Detonation.find(level, BlockPos.of(packed));
                if (target != null
                        && Detonation.fire(
                                        level, target, player, player.getDisplayName().getString())
                                .success()) {
                    fired++;
                }
            }
        }
        EntitySounds.play(
                player,
                player.blockPosition(),
                ModSounds.TECH_BLEEP.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        if (!level.isClientSide()) {
            message(
                    player,
                    Component.translatable(
                                    "item.hbm.detonator_multi.triggered", fired, linked.size())
                            .withStyle(ChatFormatting.YELLOW));
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
                        "tooltip.hbm.detonator.add_position",
                        KeyHints.key("key.sneak", "key.keyboard.left.shift"),
                        KeyHints.key("key.use", "key.mouse.right")));
        adder.accept(
                Component.translatable(
                        "tooltip.hbm.detonator.detonate",
                        KeyHints.key("key.use", "key.mouse.right")));
        adder.accept(
                Component.translatable(
                        "tooltip.hbm.detonator.clear_positions",
                        KeyHints.key("key.sneak", "key.keyboard.left.shift"),
                        KeyHints.key("key.use", "key.mouse.right")));

        List<Long> linked = positions(stack);
        if (linked.isEmpty()) {
            adder.accept(
                    Component.translatable("item.hbm.detonator_multi.nopos")
                            .withStyle(ChatFormatting.RED));
            return;
        }
        for (long packed : linked) {
            BlockPos pos = BlockPos.of(packed);
            adder.accept(
                    Component.translatable(
                                    "item.hbm.detonator_multi.pos",
                                    pos.getX(),
                                    pos.getY(),
                                    pos.getZ())
                            .withStyle(ChatFormatting.YELLOW));
        }
    }
}
