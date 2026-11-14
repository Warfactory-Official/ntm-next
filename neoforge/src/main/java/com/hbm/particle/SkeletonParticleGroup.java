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

import com.hbm.client.render.ParticleRenderTypes;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;

public class SkeletonParticleGroup extends ParticleGroup<SkeletonParticle> {

    private final Identifier texture;

    public SkeletonParticleGroup(ParticleEngine engine, Identifier texture) {
        super(engine);
        this.texture = texture;
    }

    @Override
    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks) {
        List<Instance> out = new ArrayList<>(this.particles.size());
        Vec3 cam = camera.position();

        for (SkeletonParticle fx : this.particles) {
            if (fx.ownsVisual()) continue;
            out.add(
                    new Instance(
                            (float) (fx.interpX(partialTicks) - cam.x),
                            (float) (fx.interpY(partialTicks) - cam.y),
                            (float) (fx.interpZ(partialTicks) - cam.z),
                            fx.yaw(partialTicks),
                            fx.pitch(partialTicks),
                            SkeletonParticle.partId(fx.part()),
                            fx.argb(partialTicks),
                            fx.light(partialTicks)));
        }

        return new State(ParticleRenderTypes.skeleton(this.texture), out);
    }

    private record Instance(
            float px, float py, float pz, float yaw, float pitch, int part, int color, int light) {}

    private record State(RenderType renderType, List<Instance> instances)
            implements ParticleGroupRenderState {

        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            if (instances.isEmpty()) return;
            PoseStack pose = new PoseStack();

            collector.submitCustomGeometry(
                    pose,
                    renderType,
                    (p, tess) -> {
                        for (Instance in : instances) {
                            pose.pushPose();
                            pose.translate(in.px, in.py, in.pz);
                            pose.mulPose((Quaternionfc) Axis.YP.rotationDegrees(in.yaw));
                            pose.mulPose((Quaternionfc) Axis.XP.rotationDegrees(in.pitch));
                            pose.mulPose((Quaternionfc) Axis.YP.rotationDegrees(-90F));
                            ResourceManager.skeleton_obj.renderPart(
                                    pose.last(), tess, in.light, in.color, in.part);
                            pose.popPose();
                        }
                    });
        }
    }
}
