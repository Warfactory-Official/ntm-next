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

package com.hbm.mixin.compat.network.neb;

import cn.ussshenzhou.notenoughbandwidth.zstd.Context;
import com.github.luben.zstd.ZstdCompressCtx;
import com.github.luben.zstd.ZstdDecompressCtx;
import com.hbm.packet.compat.NebCodec;
import com.hbm.packet.compat.NebCompression;
import io.netty.buffer.ByteBuf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Context.class, remap = false)
public abstract class MixinContext implements NebCodec {
    @Shadow @Final private ZstdCompressCtx compressCtx;
    @Shadow @Final private ZstdDecompressCtx decompressCtx;
    @Shadow @Final private boolean useContext;

    @Unique private volatile NebCompression hbm$compression;

    @Unique
    private NebCompression hbm$compression() {
        NebCompression compression = hbm$compression;
        if (compression == null)
            synchronized (this) {
                compression = hbm$compression;
                if (compression == null) {
                    hbm$compression =
                            compression =
                                    new NebCompression(compressCtx, decompressCtx, useContext);
                }
            }
        return compression;
    }

    @Override
    public void hbm$compress(ByteBuf input, ByteBuf output) {
        hbm$compression().compress(input, output);
    }

    @Override
    public ByteBuf hbm$decompress(ByteBuf input, int size) {
        return hbm$compression().decompress(input, size);
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void hbm$closed(CallbackInfo ci) {
        hbm$compression = null;
    }
}
