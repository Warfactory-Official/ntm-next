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

import com.hbm.client.render.WeaponRenderTypes;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class MukeCloudParticleGroup extends ParticleGroup<MukeCloudParticle> {

    private static final float SCALE = 3F;
    private static final int LIGHT = LightCoordsUtil.pack(15, 0);

    public MukeCloudParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks) {
        List<Instance> out = new ArrayList<>(this.particles.size());
        Vec3 cam = camera.position();

        for (MukeCloudParticle cloud : this.particles) {
            if (cloud.ownsVisual()) continue;
            out.add(
                    new Instance(
                            cloud.interpX(partialTicks) - cam.x,
                            cloud.interpY(partialTicks) - cam.y,
                            cloud.interpZ(partialTicks) - cam.z,
                            cloud.texIndex(partialTicks),
                            cloud.texture()));
        }

        return new State(out);
    }

    private record Instance(
            double relX, double relY, double relZ, int texIndex, Identifier texture) {}

    private record State(List<Instance> instances) implements ParticleGroupRenderState {

        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            PoseStack pose = new PoseStack();

            for (Instance in : instances) {
                pose.pushPose();
                pose.translate(in.relX, in.relY, in.relZ);

                float f0 = 1F / 5F;
                float uMin = in.texIndex % 5 * f0;
                float uMax = uMin + f0;
                float vMin = in.texIndex / 5 * f0;
                float vMax = vMin + f0;

                Vector3f c1 = camera.orientation.transform(new Vector3f(-SCALE, -SCALE, 0));
                c1.y = -SCALE;
                Vector3f c2 = camera.orientation.transform(new Vector3f(-SCALE, SCALE, 0));
                c2.y = SCALE;
                Vector3f c3 = camera.orientation.transform(new Vector3f(SCALE, SCALE, 0));
                c3.y = SCALE;
                Vector3f c4 = camera.orientation.transform(new Vector3f(SCALE, -SCALE, 0));
                c4.y = -SCALE;

                collector.submitCustomGeometry(
                        pose,
                        WeaponRenderTypes.flashLit(in.texture),
                        (p, tess) -> {
                            Vertices.emit(
                                    tess, p, c4.x, c4.y, c4.z, -1, uMax, vMax, LIGHT, 0F, 1F, 0F);
                            Vertices.emit(
                                    tess, p, c3.x, c3.y, c3.z, -1, uMax, vMin, LIGHT, 0F, 1F, 0F);
                            Vertices.emit(
                                    tess, p, c2.x, c2.y, c2.z, -1, uMin, vMin, LIGHT, 0F, 1F, 0F);
                            Vertices.emit(
                                    tess, p, c1.x, c1.y, c1.z, -1, uMin, vMax, LIGHT, 0F, 1F, 0F);
                        });
                pose.popPose();
            }
        }
    }
}
