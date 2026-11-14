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

package com.hbm.client.particle;

import dev.engine_room.flywheel.api.visual.Effect;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public final class RadiationFogEffect implements Effect {

    static final int QUAD_COUNT = RadiationFogPattern.QUAD_COUNT;
    static final double[] OFF_X = RadiationFogPattern.OFF_X;
    static final double[] OFF_Y = RadiationFogPattern.OFF_Y;
    static final double[] OFF_Z = RadiationFogPattern.OFF_Z;
    static final double[] JIT_X = RadiationFogPattern.JIT_X;
    static final double[] JIT_Y = RadiationFogPattern.JIT_Y;
    static final double[] JIT_Z = RadiationFogPattern.JIT_Z;
    static final float[] SIZE = RadiationFogPattern.SIZE;
    private static final int LIFETIME = 400;
    private static final int PEAK_RGB = (217 << 16) | (230 << 8) | 127;
    private static final float PEAK_ALPHA = 0.125F;
    final Level level;
    final double x, y, z;
    int age;

    public RadiationFogEffect(Level level, double x, double y, double z) {
        this.level = level;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public Level level() {
        return level;
    }

    @Override
    public EffectVisual<?> visualize(VisualizationContext ctx, float partialTick) {
        return new RadiationFogVisual(ctx, this);
    }

    int colorAt(float ageFloat) {
        float t = Mth.clamp(ageFloat / (float) LIFETIME, 0F, 1F);
        float alpha = (float) Math.sin(t * Math.PI) * PEAK_ALPHA;
        return ARGB.color(ARGB.as8BitChannel(alpha), PEAK_RGB);
    }

    boolean expired() {
        return age >= LIFETIME;
    }
}
