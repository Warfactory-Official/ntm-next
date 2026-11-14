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

package com.hbm.mixin.c2me;

import com.hbm.util.ChunkPayloads;
import com.ishland.c2me.rewrites.chunk_serializer.common.ChunkDataSerializer;
import com.ishland.c2me.rewrites.chunk_serializer.common.NbtWriter;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChunkDataSerializer.class, remap = false)
public abstract class MixinChunkDataSerializer {
    @Inject(method = "write", at = @At("RETURN"))
    private static void hbm$writePayloads(
            SerializableChunkData data, NbtWriter writer, CallbackInfo _) {
        ChunkPayloads.write(data, writer::putElement);
    }
}
