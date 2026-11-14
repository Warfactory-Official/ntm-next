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

import com.hbm.particle.AshRevealParticle;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public final class AshRevealVisual
        implements EffectVisual<AshRevealParticle>, SimpleTickableVisual, ShaderLightVisual {
    private final AshRevealParticle particle;
    private final InstanceParticle instance;
    private final Vec3i origin;
    private @Nullable SectionCollector sections;
    private long sectionMin = Long.MIN_VALUE, sectionMax;

    public AshRevealVisual(VisualizationContext context, AshRevealParticle particle) {
        this.particle = particle;
        origin = context.renderOrigin();
        instance =
                context.instancerProvider()
                        .instancer(InstanceParticle.GROW_IN, ParticleModels.LIT)
                        .createInstance();
        writeTick();
    }

    @Override
    public void tick(Context context) {
        writeTick();
    }

    private void writeTick() {
        instance.setVisible(particle.isAlive());
        if (!particle.isAlive()) return;
        instance.previousPosition(
                (float) (particle.xo - origin.getX()),
                (float) (particle.yo - origin.getY()),
                (float) (particle.zo - origin.getZ()));
        instance.position(
                (float) (particle.x - origin.getX()),
                (float) (particle.y - origin.getY()),
                (float) (particle.z - origin.getZ()));
        instance.size(2 * particle.quadSize);
        instance.animation(particle.oRoll, particle.roll, particle.age, particle.getLifetime());
        instance.uvRegion(
                particle.visualU0(),
                particle.visualV0(),
                particle.visualU1() - particle.visualU0(),
                particle.visualV1() - particle.visualV0());
        instance.color(particle.rCol, particle.gCol, particle.bCol, particle.alpha)
                .light(0)
                .setChanged();
        publishLight();
    }

    private void publishLight() {
        if (sections == null) return;
        double radius = particle.quadSize * 1.415 + 1;
        int x0 = Mth.floor(Math.min(particle.xo, particle.x) - radius) >> 4;
        int y0 = Mth.floor(Math.min(particle.yo, particle.y) - radius) >> 4;
        int z0 = Mth.floor(Math.min(particle.zo, particle.z) - radius) >> 4;
        int x1 = Mth.floor(Math.max(particle.xo, particle.x) + radius) >> 4;
        int y1 = Mth.floor(Math.max(particle.yo, particle.y) + radius) >> 4;
        int z1 = Mth.floor(Math.max(particle.zo, particle.z) + radius) >> 4;
        long min = SectionPos.asLong(x0, y0, z0), max = SectionPos.asLong(x1, y1, z1);
        if (min == sectionMin && max == sectionMax) return;
        sectionMin = min;
        sectionMax = max;
        var required = new LongOpenHashSet();
        for (int x = x0; x <= x1; x++)
            for (int y = y0; y <= y1; y++)
                for (int z = z0; z <= z1; z++) required.add(SectionPos.asLong(x, y, z));
        sections.sections(required);
    }

    @Override
    public void setSectionCollector(SectionCollector collector) {
        sections = collector;
        publishLight();
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        instance.delete();
    }
}
