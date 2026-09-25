// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.effect.EntityNukeTorex.Cloudlet;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.interfaces.injected.IBufferBuilderExtension;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public class RenderTorex extends EntityRenderer<EntityNukeTorex, RenderTorex.State> {

    public static final int flashBaseDuration = 30;
    public static final int flareBaseDuration = 100;

    public RenderTorex(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    public static int cloudletShading(Cloudlet c, float partialTicks) {
        float alpha = c.getAlpha();
        float brightness =
                c.type == EntityNukeTorex.TorexType.CONDENSATION ? 0.9F : 0.75F * c.colorMod;
        double greying = c.type == EntityNukeTorex.TorexType.RING ? 0.05D : 0D;
        double cr, cg, cb;
        if (c.type == EntityNukeTorex.TorexType.CONDENSATION) {
            cr = cg = cb = 1D;
        } else {
            cr = c.prevColorR + (c.colorR - c.prevColorR) * partialTicks + greying;
            cg = c.prevColorG + (c.colorG - c.prevColorG) * partialTicks + greying;
            cb = c.prevColorB + (c.colorB - c.prevColorB) * partialTicks + greying;
        }
        float r = Mth.clamp((float) cr * brightness, 0.15F, 1F);
        float g = Mth.clamp((float) cg * brightness, 0.15F, 1F);
        float b = Mth.clamp((float) cb * brightness, 0.15F, 1F);
        return ARGB.colorFromFloat(alpha, r, g, b);
    }

    private static void renderCloudlets(
            PoseStack.Pose pose, VertexConsumer buf, State state, Vector3f right, Vector3f up) {
        int n = state.cloudletCount;

        IBufferBuilderExtension block =
                buf instanceof IBufferBuilderExtension fast && fast.hbm$beginBlock(n * 4)
                        ? fast
                        : null;
        for (int i = 0; i < n; i++) {
            billboard(
                    buf,
                    block,
                    pose,
                    state.cx[i],
                    state.cy[i],
                    state.cz[i],
                    state.csize[i],
                    right,
                    up,
                    state.ccolor[i],
                    LightCoordsUtil.FULL_BRIGHT);
        }
        if (block != null) block.hbm$endBlock();
    }

    private static void renderFlare(
            PoseStack.Pose pose,
            VertexConsumer buf,
            State state,
            float flareDuration,
            Vector3f right,
            Vector3f up) {
        double age = Math.min(state.age, flareDuration);
        float alpha = (float) Math.min(1D, (flareDuration - age) / flareDuration);
        int color = ARGB.colorFromFloat(alpha, 1F, 1F, 1F);
        Random rand = new Random(state.entityId);
        float size = 10F * state.rollerSize;
        for (int i = 0; i < 3; i++) {
            float x = (float) (rand.nextGaussian() * 0.5F * state.rollerSize);
            float y = (float) (rand.nextGaussian() * 0.5F * state.rollerSize);
            float z = (float) (rand.nextGaussian() * 0.5F * state.rollerSize);
            billboard(
                    buf,
                    null,
                    pose,
                    x,
                    y + state.coreHeight,
                    z,
                    size,
                    right,
                    up,
                    color,
                    LightCoordsUtil.FULL_BRIGHT);
        }
    }

    private static void renderFlash(
            PoseStack.Pose pose, VertexConsumer buf, State state, float flashDuration) {
        double intensityRaw = state.age / flashDuration;

        double intensity = intensityRaw * Math.pow(Math.E, -intensityRaw) * 2.717391304D;
        float scale = 50F * state.torexScale;
        float oy = 0.8F * state.coreHeight;
        float k = (float) (intensity * scale);
        int centerColor = ARGB.colorFromFloat((float) (1D - intensity), 1F, 1F, 1F);
        int rimColor = ARGB.colorFromFloat(0F, 1F, 1F, 1F);
        float[] corners = TorexFlash.CORNERS;
        for (int i = 0; i < TorexFlash.CONES; i++) {
            int at = i * 9;
            float x1 = k * corners[at], y1 = k * corners[at + 1] + oy, z1 = k * corners[at + 2];
            float x2 = k * corners[at + 3], y2 = k * corners[at + 4] + oy, z2 = k * corners[at + 5];
            float x3 = k * corners[at + 6], y3 = k * corners[at + 7] + oy, z3 = k * corners[at + 8];

            flashTri(buf, pose, oy, x1, y1, z1, x2, y2, z2, centerColor, rimColor);
            flashTri(buf, pose, oy, x2, y2, z2, x3, y3, z3, centerColor, rimColor);
            flashTri(buf, pose, oy, x3, y3, z3, x1, y1, z1, centerColor, rimColor);
        }
    }

    private static void flashTri(
            VertexConsumer buf,
            PoseStack.Pose pose,
            float oy,
            float bx,
            float by,
            float bz,
            float cx,
            float cy,
            float cz,
            int apexColor,
            int rimColor) {
        Vertices.emit(buf, pose, 0F, oy, 0F, apexColor);
        Vertices.emit(buf, pose, bx, by, bz, rimColor);
        Vertices.emit(buf, pose, cx, cy, cz, rimColor);
        Vertices.emit(buf, pose, cx, cy, cz, rimColor);
    }

    private static void billboard(
            VertexConsumer buf,
            @Nullable IBufferBuilderExtension block,
            PoseStack.Pose pose,
            float cx,
            float cy,
            float cz,
            float size,
            Vector3f right,
            Vector3f up,
            int color,
            int light) {
        float sx = right.x * size, sy = right.y * size, sz = right.z * size;
        float ax = up.x * size, ay = up.y * size, az = up.z * size;
        vtx(buf, block, pose, cx - sx - ax, cy - sy - ay, cz - sz - az, 0F, 1F, color, light);
        vtx(buf, block, pose, cx + sx - ax, cy + sy - ay, cz + sz - az, 1F, 1F, color, light);
        vtx(buf, block, pose, cx + sx + ax, cy + sy + ay, cz + sz + az, 1F, 0F, color, light);
        vtx(buf, block, pose, cx - sx + ax, cy - sy + ay, cz - sz + az, 0F, 0F, color, light);
    }

    private static void vtx(
            VertexConsumer buf,
            @Nullable IBufferBuilderExtension block,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            int color,
            int light) {
        if (block != null) {
            Vertices.emitBlock(
                    block,
                    pose,
                    x,
                    y,
                    z,
                    color,
                    u,
                    v,
                    OverlayTexture.NO_OVERLAY,
                    light,
                    0F,
                    1F,
                    0F);
            return;
        }
        Vertices.emit(buf, pose, x, y, z, color, u, v, light, 0F, 1F, 0F);
    }

    @Override
    protected boolean affectedByCulling(EntityNukeTorex entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityNukeTorex entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tickCount = entity.age();

        state.age = entity.age() + partialTicks;
        state.torexScale = (float) entity.getScaleVal();
        state.coreHeight = (float) entity.coreHeight;
        state.rollerSize = (float) entity.rollerSize;
        state.entityId = entity.getId();
        double ex = entity.getX(), ey = entity.getY(), ez = entity.getZ();

        if (entity.lastRenderSortTick != entity.tickCount) {
            Vec3 cam = Minecraft.getInstance().gameRenderer.mainCamera().position();
            for (Cloudlet c : entity.cloudlets) {
                double dx = cam.x - c.posX, dy = cam.y - c.posY, dz = cam.z - c.posZ;
                c.renderSortDistanceSq = dx * dx + dy * dy + dz * dz;
            }
            entity.cloudlets.sort(
                    (a, b) -> Double.compare(b.renderSortDistanceSq, a.renderSortDistanceSq));
            entity.lastRenderSortTick = entity.tickCount;
        }

        int n = entity.cloudlets.size();
        state.alloc(n);
        state.cloudletCount = n;
        for (int i = 0; i < n; i++) {
            Cloudlet c = entity.cloudlets.get(i);
            state.cx[i] = (float) (c.prevPosX + (c.posX - c.prevPosX) * partialTicks - ex);
            state.cy[i] = (float) (c.prevPosY + (c.posY - c.prevPosY) * partialTicks - ey);
            state.cz[i] = (float) (c.prevPosZ + (c.posZ - c.prevPosZ) * partialTicks - ez);
            state.csize[i] = c.getScale();
            state.ccolor[i] = cloudletShading(c, partialTicks);
        }
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {

        Quaternionf q = camera.orientation;
        Vector3f right = q.transform(new Vector3f(1F, 0F, 0F));
        Vector3f up = q.transform(new Vector3f(0F, 1F, 0F));

        if (state.cloudletCount > 0) {
            collector
                    .order(0)
                    .submitCustomGeometry(
                            poseStack,
                            TorexRenderTypes.CLOUDLET,
                            (pose, buf) -> renderCloudlets(pose, buf, state, right, up));
        }

        float flareDuration = state.torexScale * flareBaseDuration;
        if (state.tickCount < flareDuration + 1) {
            collector
                    .order(1)
                    .submitCustomGeometry(
                            poseStack,
                            TorexRenderTypes.FLARE,
                            (pose, buf) -> renderFlare(pose, buf, state, flareDuration, right, up));
        }

        float flashDuration = state.torexScale * flashBaseDuration;
        if (state.tickCount < flashDuration) {
            collector
                    .order(2)
                    .submitCustomGeometry(
                            poseStack,
                            TorexRenderTypes.FLASH,
                            (pose, buf) -> renderFlash(pose, buf, state, flashDuration));
        }

        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        int tickCount;
        float age;
        float torexScale;
        float coreHeight;
        float rollerSize;
        int entityId;
        int cloudletCount;
        float[] cx;
        float[] cy;
        float[] cz;
        float[] csize;
        int[] ccolor;

        void alloc(int n) {
            cx = new float[n];
            cy = new float[n];
            cz = new float[n];
            csize = new float[n];
            ccolor = new int[n];
        }
    }
}
