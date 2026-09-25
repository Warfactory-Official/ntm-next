// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.hbm.client.render.ParticleRenderTypes;
import com.hbm.main.ResourceManager;
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
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

public class RiftParticleGroup extends ParticleGroup<ParticleRift> {

    public RiftParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks) {
        List<float[]> out = new ArrayList<>(this.particles.size());
        Vec3 cam = camera.position();

        for (ParticleRift fx : this.particles) {
            out.add(
                    new float[] {
                        (float) (fx.interpX(partialTicks) - cam.x),
                        (float) (fx.interpY(partialTicks) - cam.y),
                        (float) (fx.interpZ(partialTicks) - cam.z),
                        fx.shellScale(partialTicks)
                    });
        }

        return new State(out);
    }

    private record State(List<float[]> instances) implements ParticleGroupRenderState {

        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            if (instances.isEmpty()) return;
            PoseStack pose = new PoseStack();

            collector.submitCustomGeometry(
                    pose,
                    ParticleRenderTypes.RIFT,
                    (p, tess) -> {
                        for (float[] in : instances) {
                            for (float shell : ParticleRift.SHELLS) {
                                float s = in[3] * shell;
                                pose.pushPose();
                                pose.translate(in[0], in[1], in[2]);
                                pose.scale(s, s, s);
                                ResourceManager.sphere_uv.render(
                                        pose.last(), tess, LightCoordsUtil.FULL_BRIGHT, 0xFFFFFFFF);
                                pose.popPose();
                            }
                        }
                    });
        }
    }
}
