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
import dev.engine_room.flywheel.api.backend.GameClock;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jspecify.annotations.Nullable;

public final class ClientGameTime {

    private static volatile @Nullable ClientLevel clockLevel;
    private static final VarHandle TICK_FLOOR;
    private static final VarHandle NOW_FLOOR;
    private static volatile long tickFloor;
    private static volatile long nowFloor;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            TICK_FLOOR = lookup.findStaticVarHandle(ClientGameTime.class, "tickFloor", long.class);
            NOW_FLOOR = lookup.findStaticVarHandle(ClientGameTime.class, "nowFloor", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private ClientGameTime() {}

    public static void install() {
        GameTime.CLIENT_MILLIS = ClientGameTime::millis;
        GameTime.CLIENT_NOW = ClientGameTime::now;
        GameClock.install(GameTime::now);
    }

    private static long millis() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return 0L;
        follow(level);
        return raise(TICK_FLOOR, level.getGameTime() * 50L);
    }

    private static long now() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return 0L;
        follow(level);
        long raw =
                level.getGameTime() * 50L
                        + (long) (mc.getDeltaTracker().getGameTimeDeltaPartialTick(false) * 50F);
        return raise(NOW_FLOOR, Math.max(raw, tickFloor));
    }

    private static void follow(ClientLevel level) {
        if (clockLevel == level) return;
        clockLevel = level;
        tickFloor = 0L;
        nowFloor = 0L;
    }

    private static long raise(VarHandle floor, long value) {
        long current;
        do {
            current = (long) floor.getVolatile();
            if (current >= value) return current;
        } while (!floor.compareAndSet(current, value));
        return value;
    }
}
