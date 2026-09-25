// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.interfaces.injected.IChunkExtension;
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
}
