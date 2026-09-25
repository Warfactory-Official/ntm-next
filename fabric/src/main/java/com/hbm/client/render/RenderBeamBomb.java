// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.projectile.EntityB92Beam;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public class RenderBeamBomb extends EntityRenderer<EntityB92Beam, RenderBeamBomb.State>
        implements ConcurrentRenderStateExtraction {

    private static final float RADIUS = 0.175F;
    private static final int LENGTH = 2;
    private static final int LAYERS = 8;

    public RenderBeamBomb(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    private static void beam(VertexConsumer buffer, PoseStack.Pose pose) {
        for (int layer = 0; layer <= LAYERS; layer++) {
            float o = RADIUS / LAYERS * layer;
            float shade = Math.max(1F - o * 8.333F, 0F);
            int color = ARGB.colorFromFloat(1F, shade, shade, 1F);
            wall(buffer, pose, color, o, -o, o, o);
            wall(buffer, pose, color, -o, -o, o, -o);
            wall(buffer, pose, color, -o, o, -o, -o);
            wall(buffer, pose, color, o, o, -o, o);
        }
    }

    private static void wall(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            int color,
            float x0,
            float y0,
            float x1,
            float y1) {
        Vertices.emit(buffer, pose, x0, y0, 0F, color);
        Vertices.emit(buffer, pose, x1, y1, 0F, color);
        Vertices.emit(buffer, pose, x1, y1, LENGTH, color);
        Vertices.emit(buffer, pose, x0, y0, LENGTH, color);
    }

    @Override
    public boolean shouldRender(
            EntityB92Beam entity, Frustum culler, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityB92Beam entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.mulPose(Axis.XN.rotationDegrees(state.pitch));
        collector.submitCustomGeometry(
                poseStack, BeamRenderTypes.ADDITIVE, (pose, buffer) -> beam(buffer, pose));
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float yaw;
        float pitch;
    }
}
