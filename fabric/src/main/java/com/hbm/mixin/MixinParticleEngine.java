// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.api.client.ParticleSpawns;
import com.hbm.client.particle.DebrisVisuals;
import com.hbm.client.particle.VisualParticle;
import java.util.Map;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
abstract class MixinParticleEngine {
    @Shadow @Final private Map<ParticleRenderType, ParticleGroup<?>> particles;

    @Inject(method = "clearParticles", at = @At("HEAD"))
    private void hbm$clearParticles(CallbackInfo ci) {
        DebrisVisuals.clear();
        for (var group : particles.values())
            for (var particle : group.particles)
                if (particle instanceof VisualParticle visual) visual.removeVisual();
    }

    @Inject(method = "createParticle", at = @At("RETURN"))
    private void hbm$addonSpawns(
            ParticleOptions options,
            double x,
            double y,
            double z,
            double xa,
            double ya,
            double za,
            CallbackInfoReturnable<Particle> cir) {
        Particle particle = cir.getReturnValue();
        if (particle != null) ParticleSpawns.created(options, x, y, z, particle);
    }
}
