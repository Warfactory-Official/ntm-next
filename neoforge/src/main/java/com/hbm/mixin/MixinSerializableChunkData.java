// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.blocks.multiblock.MultiblockCoreIndex;
import com.hbm.interfaces.injected.IChunkExtension;
import com.hbm.util.datafix.HbmDataFixers;
import java.util.Arrays;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SerializableChunkData.class)
public abstract class MixinSerializableChunkData implements IChunkExtension {

    @Unique private byte @Nullable [] hbm$radiation;

    @Inject(method = "parse", at = @At("RETURN"))
    private static void hbm$captureRadiation(
            LevelHeightAccessor levelHeight,
            PalettedContainerFactory containerFactory,
            CompoundTag chunkData,
            CallbackInfoReturnable<SerializableChunkData> cir) {
        SerializableChunkData data = cir.getReturnValue();
        if (data != null) {
            data.hbm$setRadiation(
                    chunkData.getByteArray(IChunkExtension.RADIATION_NBT_KEY).orElse(null));
        }
    }

    @Override
    public byte @Nullable [] hbm$getRadiation() {
        return this.hbm$radiation;
    }

    @Override
    public void hbm$setRadiation(byte @Nullable [] bytes) {
        this.hbm$radiation = bytes;
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void hbm$carryRadiation(
            ServerLevel level,
            PoiManager poiManager,
            RegionStorageInfo regionInfo,
            ChunkPos pos,
            CallbackInfoReturnable<ProtoChunk> cir) {
        if (this.hbm$radiation == null) return;
        ProtoChunk result = cir.getReturnValue();
        ChunkAccess target =
                (result instanceof ImposterProtoChunk imposter) ? imposter.getWrapped() : result;
        target.hbm$setRadiation(this.hbm$radiation);
    }

    @Unique private long @Nullable [] hbm$coreIndex;
    @Unique private int hbm$coreIndexSize;

    @Inject(method = "copyOf", at = @At("RETURN"))
    private static void hbm$captureCoreIndex(
            ServerLevel level,
            ChunkAccess chunk,
            CallbackInfoReturnable<SerializableChunkData> cir) {
        SerializableChunkData data = cir.getReturnValue();
        if (data == null) return;
        long[] entries = MultiblockCoreIndex.copyForSave(chunk);
        data.hbm$setCoreIndex(entries, entries == null ? 0 : entries.length);
    }

    @Inject(method = "parse", at = @At("RETURN"))
    private static void hbm$parseCoreIndex(
            LevelHeightAccessor levelHeight,
            PalettedContainerFactory containerFactory,
            CompoundTag chunkData,
            CallbackInfoReturnable<SerializableChunkData> cir) {
        SerializableChunkData data = cir.getReturnValue();
        if (data == null) return;
        long[] entries = chunkData.getLongArray(IChunkExtension.CORE_INDEX_NBT_KEY).orElse(null);
        if (entries != null) data.hbm$setCoreIndex(entries, entries.length);
    }

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

    @Inject(method = "write", at = @At("RETURN"))
    private void hbm$writeCoreIndex(CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        if (tag == null) return;
        HbmDataFixers.stamp(tag);
        long[] entries = this.hbm$coreIndex;
        if (entries == null || this.hbm$coreIndexSize == 0) return;
        tag.putLongArray(
                IChunkExtension.CORE_INDEX_NBT_KEY,
                this.hbm$coreIndexSize == entries.length
                        ? entries
                        : Arrays.copyOf(entries, this.hbm$coreIndexSize));
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void hbm$readCoreIndex(
            ServerLevel level,
            PoiManager poiManager,
            RegionStorageInfo regionInfo,
            ChunkPos pos,
            CallbackInfoReturnable<ProtoChunk> cir) {
        if (this.hbm$coreIndex == null) return;
        ProtoChunk result = cir.getReturnValue();
        ChunkAccess target =
                (result instanceof ImposterProtoChunk imposter) ? imposter.getWrapped() : result;
        target.hbm$setCoreIndex(this.hbm$coreIndex, this.hbm$coreIndexSize);
    }
}
