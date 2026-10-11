// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.network.bo;

import com.PinkCats.bandwidthoptimizer.channel.algorithm.zstd.KineticStreamingLayer;
import com.hbm.packet.compat.BoStreamBuffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KineticStreamingLayer.class, remap = false)
public abstract class MixinKineticStreamingLayer {
    @Unique private BoStreamBuffers hbm$buffers;

    @Shadow
    public abstract void reset();

    @Unique
    private BoStreamBuffers hbm$buffers() {
        if (hbm$buffers == null) hbm$buffers = new BoStreamBuffers(this);
        return hbm$buffers;
    }

    @Overwrite
    public byte[] encode(byte[] inputBytes) {
        return hbm$buffers().encode(inputBytes);
    }

    @Overwrite
    public byte[] decode(byte[] inputBytes) {
        try {
            return hbm$buffers().decode(inputBytes);
        } catch (RuntimeException failure) {
            reset();
            throw failure;
        }
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void hbm$closed(CallbackInfo ci) {
        hbm$buffers = null;
    }
}
