// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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

public class MukeCloudParticleGroup extends ParticleGroup<ParticleMukeCloud> {

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

        for (ParticleMukeCloud cloud : this.particles) {
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
                        WeaponRenderTypes.cloudSeparate(in.texture),
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
