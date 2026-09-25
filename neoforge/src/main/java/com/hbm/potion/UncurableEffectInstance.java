// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.potion;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public final class UncurableEffectInstance extends MobEffectInstance {

    public UncurableEffectInstance(Holder<MobEffect> effect, int duration) {
        super(effect, duration);
    }

    public UncurableEffectInstance(Holder<MobEffect> effect, int duration, int amplifier) {
        super(effect, duration, amplifier);
    }

    public UncurableEffectInstance(
            Holder<MobEffect> effect,
            int duration,
            int amplifier,
            boolean ambient,
            boolean visible) {
        super(effect, duration, amplifier, ambient, visible);
    }
}
