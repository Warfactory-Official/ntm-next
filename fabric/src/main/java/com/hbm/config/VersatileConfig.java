// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.config;

import com.hbm.data.ExplosionData;
import com.hbm.data.ItemData;
import com.hbm.platform.Services;
import com.hbm.potion.HbmPotion;
import com.hbm.potion.UncurableEffectInstance;
import java.util.Locale;
import net.minecraft.SharedConstants;
import net.minecraft.world.entity.LivingEntity;

public final class VersatileConfig {

    private static final int MINUTE = 60 * 20;
    private static final int HOUR = 60 * MINUTE;

    private VersatileConfig() {}

    public static int getSchrabOreChance() {
        return BalanceConfig.enableLBSM ? ExplosionData.SCHRAB_ORE_RATE.get() : 250;
    }

    public static int getLongDecayChance() {
        return BalanceConfig.enable528 ? 15 * HOUR : shorterDecay() ? 15 * MINUTE : 3 * HOUR;
    }

    public static int getShortDecayChance() {
        return BalanceConfig.enable528 ? 3 * HOUR : shorterDecay() ? 3 * MINUTE : 15 * MINUTE;
    }

    private static boolean shorterDecay() {
        return BalanceConfig.enableLBSM && BalanceConfig.enableLBSMShorterDecay;
    }

    public static void applyPotionSickness(LivingEntity entity, int duration) {
        int mode = potionSicknessMode();
        if (mode == 0) return;
        int seconds = mode == 2 ? duration * 12 : duration;
        entity.addEffect(
                new UncurableEffectInstance(
                        HbmPotion.potionsickness(), seconds * SharedConstants.TICKS_PER_SECOND));
    }

    private static int potionSicknessMode() {
        String s = ItemData.POTION_SICKNESS.get().toLowerCase(Locale.US);
        if ("normal".equals(s)) return 1;
        if ("terraria".equals(s)) return 2;
        return 0;
    }

    public static boolean hasPotionSickness(LivingEntity entity) {
        return entity.hasEffect(HbmPotion.potionsickness());
    }
}
