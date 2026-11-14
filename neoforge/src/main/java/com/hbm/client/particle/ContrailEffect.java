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

import com.hbm.particle.ContrailParticle;
import dev.engine_room.flywheel.api.visual.Effect;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import java.util.Random;
import net.minecraft.world.level.Level;

public final class ContrailEffect implements Effect {
    private static int seedSeq;

    final float[] mod = new float[ContrailParticle.SUBQUADS];
    final double[] gaussX = new double[ContrailParticle.SUBQUADS];
    final double[] gaussY = new double[ContrailParticle.SUBQUADS];
    final double[] gaussZ = new double[ContrailParticle.SUBQUADS];
    final double x, y, z;
    final float r, g, b, scale;
    final int maxAge;
    private final Level level;
    int age;

    public ContrailEffect(
            Level level,
            double x,
            double y,
            double z,
            float r,
            float g,
            float b,
            float scale,
            int maxAge) {
        this.level = level;
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
        this.g = g;
        this.b = b;
        this.scale = scale;
        this.maxAge = maxAge;

        Random urandom = new Random(seedSeq++);
        for (int i = 0; i < ContrailParticle.SUBQUADS; i++) {
            this.mod[i] = urandom.nextFloat() * 0.2F + 0.2F;
            this.gaussX[i] = urandom.nextGaussian();
            this.gaussY[i] = urandom.nextGaussian();
            this.gaussZ[i] = urandom.nextGaussian();
        }
    }

    @Override
    public Level level() {
        return level;
    }

    @Override
    public EffectVisual<?> visualize(VisualizationContext ctx, float partialTick) {
        return new ContrailVisual(ctx, this);
    }

    boolean expired() {
        return age >= maxAge;
    }

    float alpha() {
        return 1F - Math.max(age - 1, 0) / (float) maxAge;
    }
}
