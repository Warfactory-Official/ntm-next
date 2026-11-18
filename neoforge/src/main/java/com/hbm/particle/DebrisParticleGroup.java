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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;

public class DebrisParticleGroup extends ParticleGroup<DebrisParticle> {

    public DebrisParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    private static RenderType layerType(ChunkSectionLayer layer) {
        return switch (layer) {
            case SOLID -> RenderTypes.solidMovingBlock();
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
        };
    }

    @Override
    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks) {
        List<Instance> out = new ArrayList<>(this.particles.size());
        Vec3 cam = camera.position();

        for (DebrisParticle fx : this.particles) {
            if (!fx.hasGeometry() || fx.isInstanced()) continue;
            DebrisChunk chunk = fx.chunk();
            out.add(
                    new Instance(
                            fx.interpX(partialTicks) - cam.x,
                            fx.interpY(partialTicks) - cam.y,
                            fx.interpZ(partialTicks) - cam.z,
                            fx.pitch(partialTicks),
                            fx.yaw(partialTicks),
                            chunk.sizeX,
                            chunk.sizeY,
                            chunk.sizeZ,
                            fx.meshes()));
        }

        return new State(out);
    }

    private record Instance(
            double relX,
            double relY,
            double relZ,
            float pitch,
            float yaw,
            int sizeX,
            int sizeY,
            int sizeZ,
            DebrisMesh[] meshes) {}

    private record State(List<Instance> instances) implements ParticleGroupRenderState {

        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            if (instances.isEmpty()) return;
            PoseStack pose = new PoseStack();
            ChunkSectionLayer[] layers = ChunkSectionLayer.values();

            for (Instance in : instances) {
                pose.pushPose();
                pose.translate(in.relX, in.relY, in.relZ);
                pose.rotate((Quaternionfc) Axis.YP.rotationDegrees(in.pitch));
                pose.rotate((Quaternionfc) Axis.ZP.rotationDegrees(in.yaw));
                pose.translate(-in.sizeX / 2D, -in.sizeY / 2D, -in.sizeZ / 2D);

                for (int i = 0; i < layers.length; i++) {
                    DebrisMesh mesh = in.meshes[i];
                    if (mesh == null) continue;
                    collector.submitCustomGeometry(
                            pose, layerType(layers[i]), (p, tess) -> mesh.emit(p, tess));
                }
                pose.popPose();
            }
        }
    }
}
