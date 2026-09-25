// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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

public class SkeletonParticleGroup extends ParticleGroup<ParticleSkeleton> {

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

        for (ParticleSkeleton fx : this.particles) {
            out.add(
                    new Instance(
                            (float) (fx.interpX(partialTicks) - cam.x),
                            (float) (fx.interpY(partialTicks) - cam.y),
                            (float) (fx.interpZ(partialTicks) - cam.z),
                            fx.yaw(partialTicks),
                            fx.pitch(partialTicks),
                            ParticleSkeleton.partId(fx.part()),
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
                            pose.mulPose(Axis.YP.rotationDegrees(in.yaw));
                            pose.mulPose(Axis.XP.rotationDegrees(in.pitch));

                            pose.mulPose(Axis.YP.rotationDegrees(-90F));
                            ResourceManager.skeleton_obj.renderPart(
                                    pose.last(), tess, in.light, in.color, in.part);
                            pose.popPose();
                        }
                    });
        }
    }
}
