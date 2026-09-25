// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.effect.EntityCloudTom;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

public class RenderCloudTom extends EntityRenderer<EntityCloudTom, RenderCloudTom.State>
        implements ConcurrentRenderStateExtraction {

    private static final RenderType TYPE =
            VortexRenderTypes.translucent(ResourceManager.tomblast_tex);
    private static final int SEGMENTS = 16;
    private static final int HEIGHT = 20;
    private static final int DEPTH = 20;
    private static final int SOLID = ARGB.color(255, 255, 255, 255);
    private static final int CLEAR = ARGB.color(0, 255, 255, 255);

    public RenderCloudTom(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    private static void curtain(
            VertexConsumer buffer, PoseStack.Pose pose, double scale, float scroll) {
        float angle = (float) Math.toRadians(360D / SEGMENTS);

        for (int i = 0; i < SEGMENTS; i++) {
            for (int j = 0; j < 5; j++) {
                double mod = 1D - j * 0.025D;
                double h = HEIGHT + j * 10D;
                double off = 1D / j;

                Vec3 vector = new Vec3(scale, 0D, 0D).yRot(angle * i);
                double x0 = vector.x * mod;
                double z0 = vector.z * mod;
                vertex(buffer, pose, x0, h, z0, 0F, (float) (1D + off) + scroll, CLEAR);
                vertex(buffer, pose, x0, -DEPTH, z0, 0F, (float) off + scroll, SOLID);

                vector = vector.yRot(angle);
                x0 = vector.x * mod;
                z0 = vector.z * mod;
                vertex(buffer, pose, x0, -DEPTH, z0, 1F, (float) off + scroll, SOLID);
                vertex(buffer, pose, x0, h, z0, 1F, (float) (1D + off) + scroll, CLEAR);
            }
        }
    }

    private static void vertex(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            double x,
            double y,
            double z,
            float u,
            float v,
            int color) {
        Vertices.emit(
                buffer,
                pose,
                (float) x,
                (float) y,
                (float) z,
                color,
                u,
                v,
                LightCoordsUtil.FULL_BRIGHT,
                0F,
                1F,
                0F);
    }

    @Override
    protected boolean affectedByCulling(EntityCloudTom entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityCloudTom entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.age + partialTicks;
        state.scroll = -(entity.tickCount + partialTicks) * 0.05F;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        double scale = state.scale;
        float scroll = state.scroll;
        collector.submitCustomGeometry(
                poseStack, TYPE, (pose, buffer) -> curtain(buffer, pose, scale, scroll));
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float scale;
        float scroll;
    }
}
