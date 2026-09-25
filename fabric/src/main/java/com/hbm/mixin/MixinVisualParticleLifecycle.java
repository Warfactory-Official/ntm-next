// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.particle.VisualParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleGroup.class)
public class MixinVisualParticleLifecycle {

    @Inject(method = "add", at = @At("RETURN"))
    private void hbm$admitted(Particle particle, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && particle instanceof VisualParticle visual)
            visual.refreshVisual();
    }

    @Inject(method = "tickParticle", at = @At("TAIL"))
    private void hbm$ticked(Particle particle, CallbackInfo ci) {
        if (particle instanceof VisualParticle visual) {
            if (particle.isAlive()) visual.refreshVisual();
            else visual.removeVisual();
        }
    }
}
