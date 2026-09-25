// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class RenderLaserColumn<T extends Entity> extends EntityRenderer<T, RenderLaserColumn.State>
        implements ConcurrentRenderStateExtraction {

    private static final int HEIGHT = 250;

    private final int outer;
    private final int inner;

    protected RenderLaserColumn(EntityRendererProvider.Context context, int outer, int inner) {
        super(context);
        this.outer = ARGB.color(255, ARGB.red(outer), ARGB.green(outer), ARGB.blue(outer));
        this.inner = ARGB.color(255, ARGB.red(inner), ARGB.green(inner), ARGB.blue(inner));
        this.shadowRadius = 0F;
    }

    static void column(
            VertexConsumer buffer, PoseStack.Pose pose, double radius, int phase, int color) {
        Vec3 vector = new Vec3(radius, 0D, 0D);
        for (int i = 0; i < phase; i++) vector = vector.yRot(45F);
        for (int i = 0; i < 8; i++) {
            Vec3 next = vector.yRot(45F);
            vertex(buffer, pose, vector.x, HEIGHT, vector.z, color);
            vertex(buffer, pose, vector.x, 0D, vector.z, color);
            vertex(buffer, pose, next.x, 0D, next.z, color);
            vertex(buffer, pose, next.x, HEIGHT, next.z, color);
            vector = next;
        }
    }

    private static void vertex(
            VertexConsumer buffer, PoseStack.Pose pose, double x, double y, double z, int color) {
        Vertices.emit(buffer, pose, (float) x, (float) y, (float) z, color);
    }

    @Override
    protected boolean affectedByCulling(T entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        RenderType type = BeamRenderTypes.ADDITIVE_CULL;
        collector.submitCustomGeometry(
                poseStack, type, (pose, buffer) -> column(buffer, pose, 0.5D, 0, outer));
        collector.submitCustomGeometry(
                poseStack, type, (pose, buffer) -> column(buffer, pose, 0.25D, 8, inner));
        super.submit(state, poseStack, collector, camera);
    }

    public static class State extends EntityRenderState {
        float age;
    }
}
