// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet;

import com.hbm.platform.Services;
import com.hbm.tileentity.Synced;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class SyncWire {

    public static final String KEY = "hbmSync";

    public static final String FRAME_KEY = "hbmSyncFrame";

    public static final String BLOB_KEY = "hbmSyncBlob";

    public static final int INITIAL_REPUBLISHES = 1;

    public static final int INITIAL_EXTRAS = 2;
    public static final ThreadLocal<ByteBuf> SCRATCH =
            ThreadLocal.withInitial(() -> Unpooled.buffer(256));
    private static long revision;
    private static volatile boolean recipesChanged;

    private static final class Replay {
        static final boolean RECORDER = Services.PLATFORM.isModLoaded("flashback");
    }

    public static boolean replayCompat() {
        return Replay.RECORDER;
    }

    public static void invalidateRecipeInputs() {
        recipesChanged = true;
    }

    public static void beforeTick() {
        if (!recipesChanged) return;
        recipesChanged = false;
        for (Synced source : ACTIVE.keySet()) source.syncChanged(3);
    }

    public static long nextRevision() {
        if (++revision <= 0) throw new IllegalStateException("Machine state revision exhausted");
        return revision;
    }

    private static final Map<Synced, Boolean> ACTIVE = new IdentityHashMap<>();
    private static final ClassValue<Integer> INITIAL_LAYOUT =
            new ClassValue<>() {
                @Override
                protected Integer computeValue(Class<?> type) {
                    return (declaredBySchema(type, "writeInitialSyncUnit", int.class, ByteBuf.class)
                                    ? INITIAL_REPUBLISHES
                                    : 0)
                            | (declaredBySchema(type, "writeInitialExtras", ByteBuf.class)
                                    ? 0
                                    : INITIAL_EXTRAS);
                }
            };

    private SyncWire() {}

    private static boolean declaredBySchema(Class<?> type, String name, Class<?>... parameters) {
        try {
            return type.getMethod(name, parameters).getDeclaringClass() == SyncUnitSchema.class;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static int initialLayout(Class<?> type) {
        return INITIAL_LAYOUT.get(type);
    }

    public static void retain(Synced source) {
        ACTIVE.put(source, Boolean.TRUE);
    }

    public static void release(Synced source) {
        ACTIVE.remove(source);
    }

    public static void clear() {
        recipesChanged = false;
        var iterator = ACTIVE.keySet().iterator();
        while (iterator.hasNext()) {
            Synced source = iterator.next();
            iterator.remove();
            source.releaseSync();
        }
    }

    public static void resendToTracking(BlockEntity entity) {
        if (entity.isRemoved()
                || !(entity instanceof Synced synced)
                || !(entity.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = entity.getBlockPos();
        var players = ChunkTrackerIndex.players(level, pos.getX() >> 4, pos.getZ() >> 4);
        for (int i = 0; i < players.size(); i++) synced.syncTo(players.get(i));
    }
}
