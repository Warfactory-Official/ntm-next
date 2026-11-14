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

package com.hbm.capability;

import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class ContractLink<T> {

    public interface Resolver<T> {
        @Nullable T at(Level level, BlockPos pos);
    }

    private final Resolver<T> resolver;
    private @Nullable T resolved;
    private @Nullable BlockEntity residency;
    private @Nullable LevelChunk resolvedChunk;
    private boolean known;
    private boolean connected;

    public ContractLink(Resolver<T> resolver) {
        this.resolver = resolver;
    }

    public boolean refresh(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            resolved = null;
            residency = null;
            resolvedChunk = null;
            known = false;
            return false;
        }
        T found = resolver.at(level, pos);
        residency = found instanceof BlockEntity be ? be : null;
        resolvedChunk = null;
        if (found != null && level instanceof ServerLevel server) {
            BlockPos at = found instanceof BlockEntity be ? be.getBlockPos() : pos;
            LevelChunk chunk = MultiblockCoreBlock.readableChunk(server, at.getX(), at.getZ());
            if (chunk == null) {
                found = null;
                residency = null;
            } else {
                resolvedChunk = chunk;
            }
        }
        resolved = found;
        boolean was = connected;
        connected = found != null;
        known = !level.isClientSide() || found != null;
        return connected != was;
    }

    public @Nullable T get(Level level, BlockPos pos) {
        if (known) {
            LevelChunk chunk = resolvedChunk;
            if (chunk != null && !chunk.getFullStatus().isOrAfter(FullChunkStatus.FULL))
                return null;
            BlockEntity be = residency;
            if (be == null || !be.isRemoved()) return resolved;
        }
        if (!known && !connected && !level.isClientSide()) return null;
        refresh(level, pos);
        return resolved;
    }

    public void load(ValueInput input, String key) {
        connected = input.getBooleanOr(key, false);
    }

    public void save(ValueOutput output, String key) {
        output.putBoolean(key, connected);
    }
}
