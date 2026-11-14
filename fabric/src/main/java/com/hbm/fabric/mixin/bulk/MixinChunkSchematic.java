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

package com.hbm.fabric.mixin.bulk;

import com.hbm.fabric.integration.bulk.BulkSchematicData;
import com.hbm.world.bulk.BulkAction;
import fi.dy.masa.litematica.world.ChunkSchematic;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChunkSchematic.class, remap = false)
public abstract class MixinChunkSchematic implements BulkSchematicData {
    @Unique
    private final Long2ObjectOpenHashMap<CompoundTag> hbm$bulkTags = new Long2ObjectOpenHashMap<>();

    @Override
    public synchronized void hbm$bulkTag(BlockPos pos, CompoundTag tag) {
        hbm$bulkTags.put(pos.asLong(), tag.copy());
    }

    @Override
    public synchronized @Nullable CompoundTag hbm$bulkTag(BlockPos pos) {
        CompoundTag tag = hbm$bulkTags.get(pos.asLong());
        return tag == null ? null : tag.copy();
    }

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void hbm$clear(
            BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
        BlockState old = cir.getReturnValue();
        if (old != null && (BulkAction.relevant(old) || BulkAction.relevant(state)))
            synchronized (this) {
                hbm$bulkTags.remove(pos.asLong());
            }
    }
}
