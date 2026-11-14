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

import com.hbm.main.ResourceManager;
import com.hbm.particle.ContrailParticle;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.BillboardInstance;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Vec3i;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

public final class ContrailVisual implements EffectVisual<ContrailEffect>, SimpleDynamicVisual {
    private static final Model MODEL = ParticleModels.texturedLit(ResourceManager.contrail_tex);
    private final ContrailEffect effect;
    private final BillboardInstance[] instances = new BillboardInstance[ContrailParticle.SUBQUADS];
    private final int[] rgb = new int[ContrailParticle.SUBQUADS];
    private float lastAlpha = Float.NaN;

    public ContrailVisual(VisualizationContext ctx, ContrailEffect effect) {
        this.effect = effect;
        Vec3i origin = ctx.renderOrigin();
        float bx = (float) (effect.x - origin.getX());
        float by = (float) (effect.y - origin.getY());
        float bz = (float) (effect.z - origin.getZ());

        Instancer<BillboardInstance> instancer =
                ctx.instancerProvider().instancer(InstanceTypes.BILLBOARD, MODEL);
        for (int i = 0; i < ContrailParticle.SUBQUADS; i++) {
            float m = effect.mod[i];
            rgb[i] =
                    channel(effect.r + m) << 16
                            | channel(effect.g + m) << 8
                            | channel(effect.b + m);

            BillboardInstance inst = instancer.createInstance();
            inst.position(
                    bx + (float) (effect.gaussX[i] * 0.5D) * effect.scale,
                    by + (float) (effect.gaussY[i] * 0.5D) * effect.scale,
                    bz + (float) (effect.gaussZ[i] * 0.5D) * effect.scale);
            inst.uvRegion(0F, 0F, 1F, 1F);
            inst.overlay(OverlayTexture.NO_OVERLAY);
            inst.light(LightCoordsUtil.pack(15, 0));
            instances[i] = inst;
        }
        writeFrame();
    }

    public static void initModels() {}

    private static int channel(float v) {
        return Mth.clamp((int) (v * 255F), 0, 255);
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        writeFrame();
    }

    private void writeFrame() {
        float alpha = effect.alpha();
        if (alpha == lastAlpha) return;
        lastAlpha = alpha;

        int a = channel(alpha);
        float size = 2F * (alpha + 0.5F) * effect.scale;
        for (int i = 0; i < ContrailParticle.SUBQUADS; i++) {
            instances[i].size(size);
            instances[i].colorArgb(a << 24 | rgb[i]);
            instances[i].setChanged();
        }
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        for (BillboardInstance instance : instances) instance.delete();
    }
}
