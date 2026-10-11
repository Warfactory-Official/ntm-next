// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.soundphysics;

import com.hbm.client.compat.SoundPhysicsOcclusion;
import com.mojang.blaze3d.audio.Channel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Channel.class)
public abstract class MixinChannel {
    @Inject(
            method = "destroy",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/AL10;alDeleteSources([I)V"))
    private void hbm$releaseEmitter(CallbackInfo ci) {
        SoundPhysicsOcclusion.release(((Channel) (Object) this).source);
    }
}
