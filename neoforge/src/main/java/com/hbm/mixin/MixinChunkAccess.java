// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.interfaces.injected.IChunkExtension;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChunkAccess.class)
public abstract class MixinChunkAccess implements IChunkExtension {

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
