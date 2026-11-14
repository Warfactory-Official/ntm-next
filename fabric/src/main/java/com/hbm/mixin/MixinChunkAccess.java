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

package com.hbm.mixin;

import com.hbm.explosion.FalloutReplay;
import com.hbm.interfaces.injected.ChunkExtension;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChunkAccess.class)
public abstract class MixinChunkAccess implements ChunkExtension {

    @Unique private long hbm$falloutAppliedThrough;
    @Unique private @Nullable FalloutReplay hbm$falloutReplay;

    @Override
    public long hbm$falloutAppliedThrough() {
        return hbm$falloutAppliedThrough;
    }

    @Override
    public void hbm$setFalloutAppliedThrough(long id) {
        hbm$falloutAppliedThrough = id;
    }

    @Override
    public @Nullable FalloutReplay hbm$falloutReplay() {
        return hbm$falloutReplay;
    }

    @Override
    public void hbm$setFalloutReplay(@Nullable FalloutReplay replay) {
        hbm$falloutReplay = replay;
    }

    @Unique private byte @Nullable [] hbm$radiation;

    @Override
    public byte @Nullable [] hbm$getRadiation() {
        return this.hbm$radiation;
    }

    @Override
    public void hbm$setRadiation(byte @Nullable [] bytes) {
        this.hbm$radiation = bytes;
    }

    @Unique private long @Nullable [] hbm$coreIndex;
    @Unique private int hbm$coreIndexSize;

    @Override
    public long @Nullable [] hbm$coreIndex() {
        return this.hbm$coreIndex;
    }

    @Override
    public int hbm$coreIndexSize() {
        return this.hbm$coreIndexSize;
    }

    @Override
    public void hbm$setCoreIndex(long @Nullable [] entries, int size) {
        this.hbm$coreIndex = entries;
        this.hbm$coreIndexSize = size;
    }

    @Unique private long @Nullable [] hbm$pendingFallerPositions;
    @Unique private BlockState @Nullable [] hbm$pendingFallerStates;
    @Unique private int hbm$pendingFallerSize;

    @Override
    public long @Nullable [] hbm$pendingFallerPositions() {
        return hbm$pendingFallerPositions;
    }

    @Override
    public BlockState @Nullable [] hbm$pendingFallerStates() {
        return hbm$pendingFallerStates;
    }

    @Override
    public int hbm$pendingFallerSize() {
        return hbm$pendingFallerSize;
    }

    @Override
    public void hbm$setPendingFallers(
            long @Nullable [] positions, BlockState @Nullable [] states, int size) {
        hbm$pendingFallerPositions = positions;
        hbm$pendingFallerStates = states;
        hbm$pendingFallerSize = size;
    }

    @Unique private @Nullable Long2ObjectOpenHashMap<String> hbm$ductFluids;

    @Override
    public @Nullable Long2ObjectOpenHashMap<String> hbm$ductFluids() {
        return this.hbm$ductFluids;
    }

    @Override
    public void hbm$setDuctFluids(@Nullable Long2ObjectOpenHashMap<String> fluids) {
        this.hbm$ductFluids = fluids;
    }
}
