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
