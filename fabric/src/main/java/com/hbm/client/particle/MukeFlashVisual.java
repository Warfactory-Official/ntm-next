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

import com.hbm.particle.MukeFlashParticle;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.BillboardInstance;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.Random;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;

public final class MukeFlashVisual implements EffectVisual<MukeFlashParticle>, SimpleDynamicVisual {
    private static final int QUADS = 24;
    private static final float[] OFFSETS = offsets();

    private final MukeFlashParticle particle;
    private final BillboardInstance[] instances = new BillboardInstance[QUADS];
    private final Vec3i origin;

    public MukeFlashVisual(
            VisualizationContext context, MukeFlashParticle particle, float partialTick) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        var instancer =
                context.instancerProvider()
                        .instancer(InstanceTypes.BILLBOARD, AdditiveParticleModels.FLARE);
        for (int i = 0; i < QUADS; i++) {
            instances[i] = instancer.createInstance();
            instances[i].uvRegion(0F, 0F, 1F, 1F).light(LightCoordsUtil.pack(15, 0));
        }
        writeFrame(partialTick);
    }

    private static float[] offsets() {
        float[] out = new float[QUADS * 3];
        Random random = new Random();
        for (int i = 0; i < QUADS; i++) {
            random.setSeed(i * 31 + 1);
            out[i * 3] = (float) (random.nextDouble() * 15D - 7.5D);
            out[i * 3 + 1] = (float) (random.nextDouble() * 7.5D - 3.75D);
            out[i * 3 + 2] = (float) (random.nextDouble() * 15D - 7.5D);
        }
        return out;
    }

    @Override
    public void beginFrame(DynamicVisual.Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        boolean visible = particle.isAlive();
        float x = (float) (particle.interpX(partialTick) - origin.getX());
        float y = (float) (particle.interpY(partialTick) - origin.getY());
        float z = (float) (particle.interpZ(partialTick) - origin.getZ());
        float s = particle.renderScale(partialTick);
        int color = ARGB.colorFromFloat(particle.alpha(partialTick) * 0.5F, 1F, 0.9F, 0.75F);
        for (int i = 0; i < QUADS; i++) {
            BillboardInstance instance = instances[i];
            instance.setVisible(visible);
            if (!visible) continue;
            instance.position(x + OFFSETS[i * 3], y + OFFSETS[i * 3 + 1], z + OFFSETS[i * 3 + 2]);
            instance.size(2F * s).colorArgb(color).setChanged();
        }
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        for (BillboardInstance instance : instances) instance.delete();
    }
}
