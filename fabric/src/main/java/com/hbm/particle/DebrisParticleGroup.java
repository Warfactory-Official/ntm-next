// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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

public class DebrisParticleGroup extends ParticleGroup<ParticleDebris> {

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

        for (ParticleDebris fx : this.particles) {
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

                pose.mulPose(Axis.YP.rotationDegrees(in.pitch));
                pose.mulPose(Axis.ZP.rotationDegrees(in.yaw));
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
