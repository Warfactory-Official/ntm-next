// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.entity;

import java.util.Arrays;
import java.util.function.Consumer;
import net.minecraft.world.entity.LivingEntity;

public final class LivingHooks {

    @SuppressWarnings("unchecked")
    private static Consumer<LivingEntity>[] tickListeners = new Consumer[0];

    private LivingHooks() {}

    public static void onTick(Consumer<LivingEntity> listener) {
        tickListeners = Arrays.copyOf(tickListeners, tickListeners.length + 1);
        tickListeners[tickListeners.length - 1] = listener;
    }

    public static void tick(LivingEntity entity) {
        for (Consumer<LivingEntity> listener : tickListeners) listener.accept(entity);
    }
}
