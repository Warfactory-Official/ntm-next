// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import it.unimi.dsi.fastutil.longs.LongList;
import java.util.concurrent.CompletableFuture;
import net.minecraft.world.level.chunk.LevelChunk;

public interface IBulkLightEngine {
    CompletableFuture<?> hbm$updateLight(LevelChunk chunk, LongList positions, long sectionMask);
}
