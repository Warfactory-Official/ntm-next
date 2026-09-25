// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.util.GameTime;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class VanishedEntities {

    private static final long DURATION = 2_000L;
    private static final Int2LongOpenHashMap STAMPS = new Int2LongOpenHashMap();

    private VanishedEntities() {}

    public static void vanish(int entityId) {
        long now = GameTime.millis();
        STAMPS.int2LongEntrySet().removeIf(e -> !live(now - e.getLongValue()));
        STAMPS.put(entityId, now);
    }

    public static boolean isVanished(Entity entity) {
        return entity instanceof LivingEntity
                && !STAMPS.isEmpty()
                && STAMPS.containsKey(entity.getId())
                && live(GameTime.now() - STAMPS.get(entity.getId()));
    }

    private static boolean live(long elapsed) {
        return elapsed >= 0L && elapsed < DURATION;
    }
}
