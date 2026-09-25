// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.hbm.client.render.WeaponRenderTypes;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class AshesParticleGroup extends ParticleGroup<ParticleAshes> {

    public AshesParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks) {
        int count = this.particles.size();

        float[] geo = new float[count * 5];

        int[] tint = new int[count * 3];
        Vec3 cam = camera.position();

        int i = 0;
        for (ParticleAshes fx : this.particles) {
            geo[i * 5] = (float) (fx.interpX(partialTicks) - cam.x);
            geo[i * 5 + 1] = (float) (fx.interpY(partialTicks) - cam.y);
            geo[i * 5 + 2] = (float) (fx.interpZ(partialTicks) - cam.z);
            geo[i * 5 + 3] = fx.size();
            geo[i * 5 + 4] = fx.spin(partialTicks);
            int shade = Mth.clamp((int) (fx.shade() * 255F), 0, 255);
            int alpha = Mth.clamp((int) (fx.alpha(partialTicks) * 255F), 0, 255);
            tint[i * 3] = alpha << 24 | shade << 16 | shade << 8 | shade;
            tint[i * 3 + 1] = fx.light(partialTicks);
            tint[i * 3 + 2] = fx.settled() ? 1 : 0;
            i++;
        }

        return new State(count, geo, tint);
    }

    private record State(int count, float[] geo, int[] tint) implements ParticleGroupRenderState {

        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            if (count == 0) return;

            Vector3f left = camera.orientation.transform(new Vector3f(-1F, 0F, 0F));
            Vector3f up = camera.orientation.transform(new Vector3f(0F, 1F, 0F));

            collector.submitCustomGeometry(
                    new PoseStack(),
                    WeaponRenderTypes.cloud(ResourceManager.particle_base_tex),
                    (p, tess) -> {
                        for (int i = 0; i < count; i++) {
                            float px = geo[i * 5], py = geo[i * 5 + 1], pz = geo[i * 5 + 2];
                            float s = geo[i * 5 + 3], spin = geo[i * 5 + 4];
                            int color = tint[i * 3], light = tint[i * 3 + 1];

                            if (tint[i * 3 + 2] != 0) {
                                flat(tess, px, py, pz, s, spin, color, light);
                            } else {
                                billboard(tess, left, up, px, py, pz, s, spin, color, light);
                            }
                        }
                    });
        }

        private static void flat(
                VertexConsumer tess,
                float px,
                float py,
                float pz,
                float s,
                float spin,
                int color,
                int light) {
            for (int c = 0; c < 4; c++) {
                float a = (spin + c * 90F) * Mth.DEG_TO_RAD;
                float cos = Mth.cos(a), sin = Mth.sin(a);

                float vx = s * cos + s * sin;
                float vz = s * cos - s * sin;
                float u = c == 0 || c == 1 ? 1F : 0F;
                float v = c == 0 || c == 3 ? 1F : 0F;
                tess.addVertex(
                        px + vx,
                        py - 0.09F,
                        pz + vz,
                        color,
                        u,
                        v,
                        OverlayTexture.NO_OVERLAY,
                        light,
                        0F,
                        1F,
                        0F);
            }
        }

        private static void billboard(
                VertexConsumer tess,
                Vector3f left,
                Vector3f up,
                float px,
                float py,
                float pz,
                float s,
                float spin,
                int color,
                int light) {
            float cos = RotatedQuad.cos(spin), sin = RotatedQuad.sin(spin);
            corner(tess, left, up, px, py, pz, -s, -s, cos, sin, color, 1F, 1F, light);
            corner(tess, left, up, px, py, pz, -s, s, cos, sin, color, 1F, 0F, light);
            corner(tess, left, up, px, py, pz, s, s, cos, sin, color, 0F, 0F, light);
            corner(tess, left, up, px, py, pz, s, -s, cos, sin, color, 0F, 1F, light);
        }

        private static void corner(
                VertexConsumer tess,
                Vector3f left,
                Vector3f up,
                float px,
                float py,
                float pz,
                float a,
                float b,
                float cos,
                float sin,
                int color,
                float u,
                float v,
                int light) {
            float la = RotatedQuad.leftOf(a, b, cos, sin);
            float ub = RotatedQuad.upOf(a, b, cos, sin);
            tess.addVertex(
                    px + left.x * la + up.x * ub,
                    py + left.y * la + up.y * ub,
                    pz + left.z * la + up.z * ub,
                    color,
                    u,
                    v,
                    OverlayTexture.NO_OVERLAY,
                    light,
                    0F,
                    1F,
                    0F);
        }
    }
}
