// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public record EntityEnchantmentLevel(
        LootContext.EntityTarget entity, Holder<Enchantment> enchantment)
        implements NumberProvider {

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
    public float getFloat(LootContext context) {
        return context.getOptionalParameter(entity.contextParam()) instanceof LivingEntity living
                ? EnchantmentHelper.getEnchantmentLevel(enchantment, living)
                : 0F;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(entity.contextParam());
    }
}
