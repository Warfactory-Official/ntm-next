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

import com.hbm.blockentity.ChunkPublication;
import com.hbm.entity.item.FallingBlockBatchEntity;
import com.hbm.entity.item.PendingFallers;
import com.hbm.explosion.FalloutJournal;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkMap.class)
public abstract class MixinChunkMap {

    @Shadow @Final private ServerLevel level;

    @Inject(method = "onFullChunkStatusChange", at = @At("TAIL"))
    private void hbm$onReadableChunkStatus(ChunkPos pos, FullChunkStatus status, CallbackInfo ci) {
        if (status != FullChunkStatus.ENTITY_TICKING)
            FallingBlockBatchEntity.freezeChunk(level, pos);
        if (!status.isOrAfter(FullChunkStatus.FULL)) {
            ChunkPublication.demote(level, pos);
            return;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x(), pos.z());
        if (chunk == null) return;
        if (status == FullChunkStatus.ENTITY_TICKING) PendingFallers.promote(level, chunk);
        ChunkPublication.promote(level, chunk);
        FalloutJournal.chunkReadable(level, chunk);
    }
}
