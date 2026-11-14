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

import com.hbm.blocks.multiblock.MultiblockCoreIndex;
import com.hbm.entity.item.PendingFallers;
import com.hbm.explosion.FalloutJournal;
import com.hbm.explosion.FalloutReplay;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.injected.ChunkExtension;
import com.hbm.util.ChunkPayloads;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Arrays;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SerializableChunkData.class)
public abstract class MixinSerializableChunkData implements ChunkExtension {
    @Unique private long hbm$falloutAppliedThrough;
    @Unique private @Nullable FalloutReplay hbm$falloutReplay;
    @Unique private byte @Nullable [] hbm$radiation;
    @Unique private long @Nullable [] hbm$coreIndex;
    @Unique private int hbm$coreIndexSize;
    @Unique private long @Nullable [] hbm$pendingFallerPositions;
    @Unique private BlockState @Nullable [] hbm$pendingFallerStates;
    @Unique private int hbm$pendingFallerSize;
    @Unique private @Nullable Long2ObjectOpenHashMap<String> hbm$ductFluids;

    @Inject(method = "copyOf", at = @At("RETURN"))
    private static void hbm$capturePayloads(
            ServerLevel level,
            ChunkAccess chunk,
            CallbackInfoReturnable<SerializableChunkData> cir) {
        SerializableChunkData data = cir.getReturnValue();
        ChunkAccess source =
                chunk instanceof ImposterProtoChunk imposter ? imposter.getWrapped() : chunk;
        ((com.hbm.interfaces.injected.ChunkExtension) (java.lang.Object) (data))
                .hbm$setFalloutAppliedThrough(
                        FalloutJournal.persistedMarker(
                                level,
                                source.getPos(),
                                ((com.hbm.interfaces.injected.ChunkExtension) (source))
                                        .hbm$falloutAppliedThrough()));
        FalloutReplay replay =
                ((com.hbm.interfaces.injected.ChunkExtension) (source)).hbm$falloutReplay();
        if (replay != null && replay.persistable())
            ((com.hbm.interfaces.injected.ChunkExtension) (java.lang.Object) (data))
                    .hbm$setFalloutReplay(replay.copy());
        long[] entries = MultiblockCoreIndex.copyForSave(chunk);
        ((com.hbm.interfaces.injected.ChunkExtension) (java.lang.Object) (data))
                .hbm$setCoreIndex(entries, entries == null ? 0 : entries.length);
        int fallers =
                ((com.hbm.interfaces.injected.ChunkExtension) (chunk)).hbm$pendingFallerSize();
        if (fallers != 0) {
            ((com.hbm.interfaces.injected.ChunkExtension) (java.lang.Object) (data))
                    .hbm$setPendingFallers(
                            Arrays.copyOf(
                                    ((com.hbm.interfaces.injected.ChunkExtension) (chunk))
                                            .hbm$pendingFallerPositions(),
                                    fallers),
                            Arrays.copyOf(
                                    ((com.hbm.interfaces.injected.ChunkExtension) (chunk))
                                            .hbm$pendingFallerStates(),
                                    fallers),
                            fallers);
        }
        Long2ObjectOpenHashMap<String> fluids =
                ((com.hbm.interfaces.injected.ChunkExtension) (chunk)).hbm$ductFluids();
        if (fluids != null && !fluids.isEmpty())
            ((com.hbm.interfaces.injected.ChunkExtension) (java.lang.Object) (data))
                    .hbm$setDuctFluids(fluids.clone());
    }

    @Inject(method = "parse", at = @At("RETURN"))
    private static void hbm$parsePayloads(
            LevelHeightAccessor levelHeight,
            PalettedContainerFactory containerFactory,
            CompoundTag chunkData,
            CallbackInfoReturnable<SerializableChunkData> cir) {
        SerializableChunkData data = cir.getReturnValue();
        if (data == null) return;
        long[] entries = chunkData.getLongArray(ChunkExtension.CORE_INDEX_NBT_KEY).orElse(null);
        if (entries != null)
            ((com.hbm.interfaces.injected.ChunkExtension) (java.lang.Object) (data))
                    .hbm$setCoreIndex(entries, entries.length);
        PendingFallers.load(chunkData.getListOrEmpty(ChunkExtension.PENDING_FALLERS_NBT_KEY), data);
        CompoundTag ducts = chunkData.getCompound(ChunkExtension.DUCT_FLUIDS_NBT_KEY).orElse(null);
        if (ducts != null) {
            long[] positions = ducts.getLongArray("pos").orElseThrow();
            ListTag ids = ducts.getListOrEmpty("fluid");
            if (ids.size() != positions.length)
                throw new IllegalStateException("duct fluids: " + ducts);
            Long2ObjectOpenHashMap<String> fluids = new Long2ObjectOpenHashMap<>(positions.length);
            for (int i = 0; i < positions.length; i++)
                fluids.put(positions[i], ids.getStringOr(i, ""));
            ((com.hbm.interfaces.injected.ChunkExtension) (java.lang.Object) (data))
                    .hbm$setDuctFluids(fluids);
        }
        ((com.hbm.interfaces.injected.ChunkExtension) (java.lang.Object) (data))
                .hbm$setFalloutAppliedThrough(
                        chunkData.getLongOr(ChunkExtension.FALLOUT_APPLIED_THROUGH_NBT_KEY, 0));
        CompoundTag replay =
                chunkData.getCompound(ChunkExtension.FALLOUT_REPLAY_NBT_KEY).orElse(null);
        if (replay != null) {
            ((com.hbm.interfaces.injected.ChunkExtension) (java.lang.Object) (data))
                    .hbm$setFalloutReplay(
                            FalloutReplay.read(
                                    replay,
                                    data.chunkPos().x(),
                                    data.chunkPos().z(),
                                    levelHeight.getMinY(),
                                    BuiltInRegistries.BLOCK));
        }
        if (!(levelHeight instanceof ServerLevel level)) return;
        ((com.hbm.interfaces.injected.ChunkExtension) (java.lang.Object) (data))
                .hbm$setRadiation(
                        RadiationSystemNT.readSidecar(level.dimension(), data.chunkPos()));
        if (data.chunkStatus().getChunkType() == ChunkType.LEVELCHUNK)
            FalloutJournal.applyParsed(level, data);
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void hbm$readPayloads(
            ServerLevel level,
            PoiManager poiManager,
            RegionStorageInfo regionInfo,
            ChunkPos pos,
            CallbackInfoReturnable<ProtoChunk> cir) {
        ProtoChunk result = cir.getReturnValue();
        ChunkAccess target =
                result instanceof ImposterProtoChunk imposter ? imposter.getWrapped() : result;
        ((com.hbm.interfaces.injected.ChunkExtension) (target))
                .hbm$setFalloutAppliedThrough(hbm$falloutAppliedThrough);
        ((com.hbm.interfaces.injected.ChunkExtension) (target))
                .hbm$setFalloutReplay(hbm$falloutReplay);
        if (hbm$radiation != null)
            ((com.hbm.interfaces.injected.ChunkExtension) (target)).hbm$setRadiation(hbm$radiation);
        if (hbm$coreIndex != null)
            ((com.hbm.interfaces.injected.ChunkExtension) (target))
                    .hbm$setCoreIndex(hbm$coreIndex, hbm$coreIndexSize);
        if (hbm$pendingFallerSize != 0) {
            ((com.hbm.interfaces.injected.ChunkExtension) (target))
                    .hbm$setPendingFallers(
                            hbm$pendingFallerPositions,
                            hbm$pendingFallerStates,
                            hbm$pendingFallerSize);
        }
        if (hbm$ductFluids != null)
            ((com.hbm.interfaces.injected.ChunkExtension) (target))
                    .hbm$setDuctFluids(hbm$ductFluids);
        FalloutJournal.afterRead(level, result);
    }

    @ModifyReturnValue(method = "write", at = @At("RETURN"))
    private CompoundTag hbm$writePayloads(CompoundTag tag) {
        ChunkPayloads.write(this, tag::put);
        return tag;
    }

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

    @Override
    public byte @Nullable [] hbm$getRadiation() {
        return hbm$radiation;
    }

    @Override
    public void hbm$setRadiation(byte @Nullable [] bytes) {
        hbm$radiation = bytes;
    }

    @Override
    public long @Nullable [] hbm$coreIndex() {
        return hbm$coreIndex;
    }

    @Override
    public int hbm$coreIndexSize() {
        return hbm$coreIndexSize;
    }

    @Override
    public void hbm$setCoreIndex(long @Nullable [] entries, int size) {
        hbm$coreIndex = entries;
        hbm$coreIndexSize = size;
    }

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

    @Override
    public @Nullable Long2ObjectOpenHashMap<String> hbm$ductFluids() {
        return hbm$ductFluids;
    }

    @Override
    public void hbm$setDuctFluids(@Nullable Long2ObjectOpenHashMap<String> fluids) {
        hbm$ductFluids = fluids;
    }
}
