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

import com.hbm.client.particle.MukeFlashVisual;
import com.hbm.client.particle.VisualParticle;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import org.jspecify.annotations.Nullable;

public class MukeFlashParticle extends Particle implements VisualParticle {

    public static final ParticleRenderType MUKE_FLASH_GROUP =
            new ParticleRenderType("hbm:muke_flash", "HMF");

    private static final int DETONATION = 15;

    public final boolean bf;
    private @Nullable VisualizationManager visualManager;

    public MukeFlashParticle(ClientLevel level, double x, double y, double z, boolean bf) {
        super(level, x, y, z);
        lifetime = 20;
        this.bf = bf;
        hasPhysics = false;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) remove();
        if (age != DETONATION) return;
        MushroomCloudEmitter.FULL.emit(
                Minecraft.getInstance().particleEngine,
                level,
                random,
                x,
                y,
                z,
                bf ? MukeCloudBFParticle::new : MukeCloudParticle::new);
    }

    public float alpha(float partialTicks) {
        return VisualParticle.linearFade(age, partialTicks, lifetime);
    }

    public float renderScale(float partialTicks) {
        return (age + partialTicks) * 3F + 1F;
    }

    public double interpX(float pt) {
        return xo + (x - xo) * pt;
    }

    public double interpY(float pt) {
        return yo + (y - yo) * pt;
    }

    public double interpZ(float pt) {
        return zo + (z - zo) * pt;
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
        return new MukeFlashVisual(context, this, partialTick);
    }

    @Override
    public ParticleRenderType getGroup() {
        return MUKE_FLASH_GROUP;
    }
}
