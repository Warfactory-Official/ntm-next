// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import com.hbm.lib.ModDamageTypes;
import com.hbm.potion.HbmPotion;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class FoodAdditives {

    public static final int CYANIDE = 1;
    public static final int RED_PILL = 2;
    private static final RandomSource RANDOM = RandomSource.create();

    private FoodAdditives() {}

    public static boolean isFood(ItemStack stack) {
        return stack.has(DataComponents.FOOD);
    }

    public static void apply(LivingEntity entity, int additives) {
        if (!(entity instanceof Player player) || !(entity.level() instanceof ServerLevel level))
            return;
        if ((additives & CYANIDE) != 0) {
            for (int i = 0; i < 10; i++) {
                player.hurtServer(
                        level,
                        level.damageSources()
                                .source(
                                        RANDOM.nextBoolean()
                                                ? ModDamageTypes.EUTHANIZED_SELF
                                                : ModDamageTypes.EUTHANIZED_SELF_2),
                        1_000F);
            }
        }
        if ((additives & RED_PILL) != 0) {
            for (int i = 0; i < 10; i++) {
                player.addEffect(
                        new MobEffectInstance(
                                HbmPotion.death(), 60 * SharedConstants.TICKS_PER_MINUTE, 0));
            }
        }
    }
}
