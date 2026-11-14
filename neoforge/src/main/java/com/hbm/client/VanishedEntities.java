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
