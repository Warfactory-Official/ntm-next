// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.potion;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.jspecify.annotations.Nullable;

public interface CloudEffect {
    static MobEffectInstance local(Holder<MobEffect> effect, int duration, int amplifier) {
        MobEffectInstance instance = new MobEffectInstance(effect, duration, amplifier);
        ((CloudEffect) instance).hbm$excludeFromCloud();
        return instance;
    }

    void hbm$excludeFromCloud();

    @Nullable MobEffectInstance hbm$cloudEffect();
}
