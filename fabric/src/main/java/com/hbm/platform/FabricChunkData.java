// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class FabricChunkData {
    public static <T> void set(ChunkAccess chunk, AttachmentType<T> key, T value) {
        chunk.setAttached(key, value);
    }
}
