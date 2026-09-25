// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.packet.SyncSource;
import io.netty.buffer.ByteBuf;
import net.minecraft.server.level.ServerPlayer;

public interface Synced extends SyncSource {

    void networkPackNT(int range);

    void networkPackNTTracking();

    void syncToTracking();

    void markChanged();

    void markSyncEvent();

    void releaseSync();

    void syncWatching(boolean watched);

    boolean applyUnitSync(long revision, long base, ByteBuf body);

    void syncTo(ServerPlayer player);
}
