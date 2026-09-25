// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.util.GameTime;
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
