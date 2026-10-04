// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import java.util.function.Supplier;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.attachment.AttachmentType;

public final class NeoForgeChunkData {
    public static <T> void set(ChunkAccess chunk, Supplier<AttachmentType<T>> key, T value) {
        chunk.setData(key.get(), value);
    }
}
