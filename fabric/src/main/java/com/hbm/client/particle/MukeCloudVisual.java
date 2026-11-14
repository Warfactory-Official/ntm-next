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

import com.hbm.particle.MukeCloudBFParticle;
import com.hbm.particle.MukeCloudParticle;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Vec3i;
import net.minecraft.util.LightCoordsUtil;

public final class MukeCloudVisual implements EffectVisual<MukeCloudParticle>, SimpleDynamicVisual {
    private static final float SIZE = 6F;

    private final MukeCloudParticle particle;
    private final InstanceParticle instance;
    private final Vec3i origin;

    public MukeCloudVisual(
            VisualizationContext context, MukeCloudParticle particle, float partialTick) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        this.instance =
                context.instancerProvider()
                        .instancer(
                                InstanceParticle.UPRIGHT,
                                particle instanceof MukeCloudBFParticle
                                        ? ParticleModels.MUKE_CLOUD_BALEFIRE
                                        : ParticleModels.MUKE_CLOUD)
                        .createInstance();
        instance.size(SIZE).light(LightCoordsUtil.pack(15, 0));
        writeFrame(partialTick);
    }

    @Override
    public void beginFrame(DynamicVisual.Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        instance.setVisible(particle.isAlive());
        if (!particle.isAlive()) return;
        float x = (float) (particle.interpX(partialTick) - origin.getX());
        float y = (float) (particle.interpY(partialTick) - origin.getY());
        float z = (float) (particle.interpZ(partialTick) - origin.getZ());
        int frame = particle.texIndex(partialTick);
        float uv = 1F / 5F;
        instance.previousPosition(x, y, z);
        instance.position(x, y, z);
        instance.animation(0F, 0F, 0F, 1F);
        instance.uvRegion(frame % 5 * uv, frame / 5 * uv, uv, uv);
        instance.color(1F, 1F, 1F, 1F).setChanged();
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        instance.delete();
    }
}
