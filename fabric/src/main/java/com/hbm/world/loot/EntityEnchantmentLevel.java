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

package com.hbm.world.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextUser;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProvider;

public record EntityEnchantmentLevel(
        LootContext.EntityTarget entity, Holder<Enchantment> enchantment)
        implements ContextFloatProvider, LootContextUser {
    public static final MapCodec<EntityEnchantmentLevel> MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            LootContext.EntityTarget.CODEC
                                                    .fieldOf("entity")
                                                    .forGetter(EntityEnchantmentLevel::entity),
                                            Enchantment.CODEC
                                                    .fieldOf("enchantment")
                                                    .forGetter(EntityEnchantmentLevel::enchantment))
                                    .apply(i, EntityEnchantmentLevel::new));

    @Override
    public MapCodec<EntityEnchantmentLevel> codec() {
        return MAP_CODEC;
    }

    @Override
    public float getFloatUnsafe(LootContext context) {
        return context.getOptional(entity.contextParam()) instanceof LivingEntity living
                ? EnchantmentHelper.getEnchantmentLevel(enchantment, living)
                : 0F;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(entity.contextParam());
    }
}
