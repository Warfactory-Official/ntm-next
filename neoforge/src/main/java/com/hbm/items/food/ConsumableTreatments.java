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

package com.hbm.items.food;

import com.hbm.config.VersatileConfig;
import com.hbm.sound.EntitySounds;
import com.hbm.util.InventoryUtil;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties.Builder;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;

public final class ConsumableTreatments {
    public static final FoodProperties MEDICINE_FOOD =
            new Builder().nutrition(0).saturationModifier(0F).alwaysEdible().build();
    public static final Consumable MEDICINE_CONSUMABLE =
            Consumable.builder().consumeSeconds(0.5F).build();
    private static Reference2ObjectOpenHashMap<Item, ConsumableTreatment> treatments =
            new Reference2ObjectOpenHashMap<>(0);
    private static Reference2ObjectOpenHashMap<Item, RandomSource> randoms =
            new Reference2ObjectOpenHashMap<>(0);

    private ConsumableTreatments() {}

    public static void applyDataPack(RegistryAccess registries) {
        var next = new Reference2ObjectOpenHashMap<Item, ConsumableTreatment>();
        var nextRandoms = new Reference2ObjectOpenHashMap<Item, RandomSource>();
        Map<Item, Identifier> claims = new HashMap<>();
        for (var holder :
                registries.lookupOrThrow(ConsumableTreatment.REGISTRY).listElements().toList()) {
            for (var target : holder.value().targets()) {
                Identifier prior = claims.putIfAbsent(target.value(), holder.key().identifier());
                if (prior != null)
                    throw new IllegalStateException(
                            "Duplicate treatment target "
                                    + target.unwrapKey().orElseThrow().identifier()
                                    + ": "
                                    + prior
                                    + " and "
                                    + holder.key().identifier());
                next.put(target.value(), holder.value());
                nextRandoms.put(target.value(), RandomSource.create());
            }
        }
        treatments = next;
        randoms = nextRandoms;
    }

    public static ConsumableTreatment forStack(ItemStack stack) {
        return treatments.get(stack.getItem());
    }

    public static InteractionResult use(
            ItemStack stack, Level level, Player player, InteractionHand hand) {
        var rule = forStack(stack);
        if (rule == null) return null;
        if (rule.blockWhenPotionSick() && VersatileConfig.hasPotionSickness(player))
            return InteractionResult.PASS;
        if (rule.trigger() != ConsumableTreatment.Trigger.USE) return null;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        Item item = stack.getItem();
        ItemStack held =
                InventoryUtil.exchangeHeld(
                        player,
                        stack,
                        rule.remainder()
                                .map(remainder -> new ItemStack(remainder.value()))
                                .orElse(ItemStack.EMPTY));
        execute(rule, item, player);
        return InteractionResult.SUCCESS.heldItemTransformedTo(held);
    }

    public static void consumed(
            ConsumableTreatment rule, Item item, Level level, LivingEntity entity) {
        if (rule != null
                && rule.trigger() == ConsumableTreatment.Trigger.CONSUME
                && level instanceof ServerLevel
                && entity instanceof Player player) execute(rule, item, player);
    }

    private static void execute(ConsumableTreatment rule, Item item, Player player) {
        rule.sound()
                .ifPresent(
                        sound ->
                                EntitySounds.play(
                                        player,
                                        player.getX(),
                                        player.getY(),
                                        player.getZ(),
                                        sound.value(),
                                        SoundSource.PLAYERS,
                                        1F,
                                        1F));
        RandomSource random = randoms.get(item);
        for (TreatmentAction step : rule.actions()) step.run(player, random);
    }

    public static void clear() {
        treatments = new Reference2ObjectOpenHashMap<>(0);
        randoms = new Reference2ObjectOpenHashMap<>(0);
    }
}
