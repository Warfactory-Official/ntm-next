// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.radiation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

interface ChunkRadiationStorage {
    byte @Nullable [] read(ServerLevel level, ChunkPos pos);

    void write(ServerLevel level, ChunkPos pos, byte @Nullable [] payload);

    void discard(ServerLevel level, ChunkPos pos);

    void flush();

    void deleteDimension(ServerLevel level);

    void close();
}
