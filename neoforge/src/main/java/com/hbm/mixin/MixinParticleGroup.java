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

import com.hbm.client.particle.VisualParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleGroup.class)
public class MixinParticleGroup {
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
