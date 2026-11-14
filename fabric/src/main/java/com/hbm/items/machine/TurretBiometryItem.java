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

package com.hbm.items.machine;

import com.hbm.items.ModDataComponents;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class TurretBiometryItem extends Item {

    public TurretBiometryItem(Properties properties) {
        super(properties);
    }

    public static String @Nullable [] getNames(ItemStack stack) {
        List<String> names = stack.get(ModDataComponents.TURRET_WHITELIST.get());
        if (names == null || names.isEmpty()) return null;
        return names.toArray(new String[0]);
    }

    public static void addName(ItemStack stack, String s) {
        List<String> names =
                new ArrayList<>(
                        stack.getOrDefault(ModDataComponents.TURRET_WHITELIST.get(), List.of()));
        if (names.contains(s)) return;
        names.add(s);
        stack.set(ModDataComponents.TURRET_WHITELIST.get(), List.copyOf(names));
    }

    public static void clearNames(ItemStack stack) {
        stack.set(ModDataComponents.TURRET_WHITELIST.get(), List.of());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        addName(stack, player.getGameProfile().name());

        if (level.isClientSide())
            player.sendSystemMessage(
                    Component.translatable("desc.item.turretBiometry.addedPlayerData"));

        EntitySounds.play(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.TECH_BLEEP.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        ((LivingEntity) player).swing(hand);

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        String[] names = getNames(stack);
        if (names == null) return;
        for (String name : names) adder.accept(Component.literal(name));
    }
}
