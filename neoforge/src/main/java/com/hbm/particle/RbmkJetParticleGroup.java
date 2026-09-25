// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.hbm.client.render.ParticleRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class RbmkJetParticleGroup extends ParticleGroup<ParticleRBMKJet> {

    private static final int LIGHT = LightCoordsUtil.pack(15, 0);

    private final Identifier texture;

    public RbmkJetParticleGroup(ParticleEngine engine, Identifier texture) {
        super(engine);
        this.texture = texture;
    }

    @Override
    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks) {
        int count = this.particles.size();

        float[] pos = new float[count * 3];

        float[] par = new float[count * 7];
        Vec3 cam = camera.position();

        int i = 0;
        for (ParticleRBMKJet fx : this.particles) {
            RbmkJetShape shape = fx.shape();
            int age = fx.clampedAge();
            pos[i * 3] = (float) (fx.interpX(partialTicks) - cam.x);
            pos[i * 3 + 1] = (float) (fx.interpY(partialTicks) - cam.y);
            pos[i * 3 + 2] = (float) (fx.interpZ(partialTicks) - cam.z);
            par[i * 7] = shape.uMin(age);
            par[i * 7 + 1] = shape.uStep();
            par[i * 7 + 2] = shape.alpha(age);
            par[i * 7 + 3] = shape.x0();
            par[i * 7 + 4] = shape.x1();
            par[i * 7 + 5] = shape.y0();
            par[i * 7 + 6] = shape.y1();
            i++;
        }

        return new State(this.texture, count, pos, par);
    }

    private record State(Identifier texture, int count, float[] pos, float[] par)
            implements ParticleGroupRenderState {

        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            if (count == 0) return;

            float yaw = camera.yRot * Mth.DEG_TO_RAD;
            float rightX = Mth.cos(yaw), rightZ = Mth.sin(yaw);

            Vector3f camUp = camera.orientation.transform(new Vector3f(0F, 1F, 0F));
            float offX = rightX, offY = camUp.y, offZ = rightZ;

            collector.submitCustomGeometry(
                    new PoseStack(),
                    ParticleRenderTypes.flashNoFogCulled(texture),
                    (p, tess) -> {
                        for (int i = 0; i < count; i++) {
                            float px = pos[i * 3] + offX,
                                    py = pos[i * 3 + 1] + offY,
                                    pz = pos[i * 3 + 2] + offZ;
                            float uMin = par[i * 7], uMax = uMin + par[i * 7 + 1];
                            int color =
                                    Mth.clamp((int) (par[i * 7 + 2] * 255F), 0, 255) << 24
                                            | 0xFFFFFF;
                            float x0 = par[i * 7 + 3], x1 = par[i * 7 + 4];
                            float y0 = par[i * 7 + 5], y1 = par[i * 7 + 6];

                            tess.addVertex(
                                    px + rightX * x0,
                                    py + y0,
                                    pz + rightZ * x0,
                                    color,
                                    uMax,
                                    1F,
                                    OverlayTexture.NO_OVERLAY,
                                    LIGHT,
                                    0F,
                                    1F,
                                    0F);
                            tess.addVertex(
                                    px + rightX * x0,
                                    py + y1,
                                    pz + rightZ * x0,
                                    color,
                                    uMax,
                                    0F,
                                    OverlayTexture.NO_OVERLAY,
                                    LIGHT,
                                    0F,
                                    1F,
                                    0F);
                            tess.addVertex(
                                    px + rightX * x1,
                                    py + y1,
                                    pz + rightZ * x1,
                                    color,
                                    uMin,
                                    0F,
                                    OverlayTexture.NO_OVERLAY,
                                    LIGHT,
                                    0F,
                                    1F,
                                    0F);
                            tess.addVertex(
                                    px + rightX * x1,
                                    py + y0,
                                    pz + rightZ * x1,
                                    color,
                                    uMin,
                                    1F,
                                    OverlayTexture.NO_OVERLAY,
                                    LIGHT,
                                    0F,
                                    1F,
                                    0F);
                        }
                    });
        }
    }
}
