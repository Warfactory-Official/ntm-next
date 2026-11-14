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

package com.hbm.particle;

import com.hbm.client.particle.MukeWaveVisual;
import com.hbm.client.particle.VisualParticle;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import org.jspecify.annotations.Nullable;

public class MukeWaveParticle extends Particle implements VisualParticle {

    public static final ParticleRenderType MUKE_WAVE_GROUP =
            new ParticleRenderType("hbm:muke_wave", "HMW");

    public float waveScale = 45F;
    private @Nullable VisualizationManager visualManager;

    public MukeWaveParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.lifetime = 25;
        this.hasPhysics = false;
    }

    public MukeWaveParticle setup(float scale, int maxAge) {
        this.waveScale = scale;
        this.lifetime = maxAge;
        return this;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) this.remove();
    }

    public float alpha(float partialTicks) {
        return VisualParticle.linearFade(this.age, partialTicks, this.lifetime);
    }

    public float renderScale(float partialTicks) {
        return (float) (1 - Math.pow(Math.E, (this.age + partialTicks) * -0.125)) * this.waveScale;
    }

    public double interpX(float pt) {
        return this.xo + (this.x - this.xo) * pt;
    }

    public double interpY(float pt) {
        return this.yo + (this.y - this.yo) * pt;
    }

    public double interpZ(float pt) {
        return this.zo + (this.z - this.zo) * pt;
    }

    @Override
    public ClientLevel level() {
        return level;
    }

    @Override
    public @Nullable VisualizationManager visualManager() {
        return visualManager;
    }

    @Override
    public void visualManager(@Nullable VisualizationManager manager) {
        visualManager = manager;
    }

    @Override
    public EffectVisual<?> visualize(VisualizationContext context, float partialTick) {
        return new MukeWaveVisual(context, this, partialTick);
    }

    @Override
    public ParticleRenderType getGroup() {
        return MUKE_WAVE_GROUP;
    }
}
