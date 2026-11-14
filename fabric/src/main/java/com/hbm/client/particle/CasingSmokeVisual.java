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

import com.hbm.client.render.RenderTextures;
import com.hbm.client.render.flywheel.PackedQuadMesh;
import com.hbm.particle.CasingParticle;
import com.hbm.particle.casing.CasingSmokeTrail;
import com.hbm.util.RegistryUtil;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleCutoutShader;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.core.Vec3i;

public final class CasingSmokeVisual implements EffectVisual<CasingParticle>, SimpleDynamicVisual {
    private static final Model MODEL =
            new SingleMeshModel(
                    PackedQuadMesh.builder(2)
                            .normal(0F, 1F, 0F)
                            .vertex(0D, 0D, 0D, 0xFFFFFFFF)
                            .vertex(1D, 0D, 0D, 0x00FFFFFF)
                            .vertex(1D, 1D, 0D, 0x00FFFFFF)
                            .vertex(0D, 1D, 0D, 0xFFFFFFFF)
                            .vertex(0D, 0D, 0D, 0xFFFFFFFF)
                            .vertex(-1D, 0D, 0D, 0x00FFFFFF)
                            .vertex(-1D, 1D, 0D, 0x00FFFFFF)
                            .vertex(0D, 1D, 0D, 0xFFFFFFFF)
                            .build(),
                    SimpleMaterial.builder()
                            .texture(RenderTextures.WHITE)
                            .mipmap(false)
                            .useOverlay(false)
                            .transparency(Transparency.ORDER_INDEPENDENT)
                            .writeMask(WriteMask.COLOR)
                            .cutout(new SimpleCutoutShader(RegistryUtil.id("cutout/zero.glsl")))
                            .light(LightShaders.FLAT)
                            .ambientOcclusion(false)
                            .cardinalLightingMode(CardinalLightingMode.ENTITY)
                            .backfaceCulling(false)
                            .build());

    private final CasingParticle particle;
    private final CasingSmokeTrail smoke;
    private final Instancer<InstanceParticle> instancer;
    private final Vec3i origin;
    private InstanceParticle[] instances = new InstanceParticle[0];
    private int active;

    public CasingSmokeVisual(VisualizationContext context, CasingParticle particle) {
        this.particle = particle;
        this.smoke = Objects.requireNonNull(particle.getSmokeTrail());
        this.origin = context.renderOrigin();
        this.instancer =
                context.instancerProvider().instancer(InstanceParticle.CASING_SMOKE, MODEL);
        writeFrame();
    }

    @Override
    public void beginFrame(DynamicVisual.Context context) {
        writeFrame();
    }

    private void writeFrame() {
        int count = particle.isAlive() ? smoke.getVisualSegmentCount() : 0;
        if (count > instances.length) {
            int old = instances.length;
            instances = Arrays.copyOf(instances, Math.max(count, old * 2 + 1));
            for (int i = old; i < instances.length; i++) {
                instances[i] = instancer.createInstance();
                instances[i].setVisible(false);
            }
        }
        if (count > 0) {
            float[] nodes = smoke.getFrame();
            float baseX = (float) (smoke.getFrameX() - origin.getX());
            float baseY = (float) (smoke.getFrameY() - origin.getY());
            float baseZ = (float) (smoke.getFrameZ() - origin.getZ());
            for (int i = 0; i < count; i++) {
                int b = i * 4;
                InstanceParticle instance = instances[i];
                instance.setVisible(true);
                instance.position(baseX + nodes[b], baseY + nodes[b + 1], baseZ + nodes[b + 2]);
                instance.previousPosition(
                        baseX + nodes[b + 4], baseY + nodes[b + 5], baseZ + nodes[b + 6]);
                instance.uvRegion(
                        smoke.getFrameHalfWidth(),
                        smoke.getFrameHalfDepth(),
                        nodes[b + 3],
                        nodes[b + 7]);
                instance.light(smoke.getFrameLight()).setChanged();
            }
        }
        for (int i = count; i < active; i++) instances[i].setVisible(false);
        active = count;
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        for (InstanceParticle instance : instances) instance.delete();
    }
}
