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

package com.hbm.platform;

import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.util.ChunkUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public abstract class ResidentLookup<T> implements BlockLookupCache<T> {

    protected final ServerLevel level;
    private @Nullable T memo;
    private @Nullable LevelChunk memoChunk;

    protected ResidentLookup(ServerLevel level) {
        this.level = level;
    }

    protected abstract @Nullable T lookup();

    protected abstract @Nullable T fresh(T found);

    @Override
    public final @Nullable T find() {
        T cached = memo;
        if (cached != null && ChunkUtil.resident((BlockEntity) cached, memoChunk)) return cached;
        invalidate();
        T found = lookup();
        if (found == null) return null;
        if (!(found instanceof BlockEntity be)) return fresh(found);
        LevelChunk chunk =
                MultiblockCoreBlock.readableChunk(
                        level, be.getBlockPos().getX(), be.getBlockPos().getZ());
        if (chunk == null || be.isRemoved()) return null;
        memo = found;
        memoChunk = chunk;
        return found;
    }

    public final void invalidate() {
        memo = null;
        memoChunk = null;
    }
}
