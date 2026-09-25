// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.effect.EntityBlackHole;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

public class RenderBlackHole<T extends EntityBlackHole>
        extends EntityRenderer<T, RenderBlackHole.State>
        implements ConcurrentRenderStateExtraction {

    private static final RenderType HOLE_TYPE = FlatCutout.of(ResourceManager.black_hole_tex);
    private static final int BLACK = ARGB.color(255, 0, 0, 0);
    private static final int GLOW = ARGB.color(191, 255, 255, 255);
    private static final int JET_CORE = ARGB.color(89, 255, 255, 255);
    private static final int JET_TIP = ARGB.color(0, 255, 255, 255);
    private static final int RING_COUNT = 16;
    private static final int DISC_STEPS = 15;

    private final Kind kind;
    private final RenderType translucent;
    private final RenderType additive;

    public RenderBlackHole(EntityRendererProvider.Context context, Kind kind) {
        super(context);
        this.kind = kind;
        this.translucent = VortexRenderTypes.translucent(kind.texture);
        this.additive = VortexRenderTypes.additive(kind.texture);
        this.shadowRadius = 0F;
    }

    private static void vertex(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            double x,
            double y,
            double z,
            double u,
            double v,
            int color) {
        Vertices.emit(
                buffer,
                pose,
                (float) x,
                (float) y,
                (float) z,
                color,
                (float) u,
                (float) v,
                LightCoordsUtil.FULL_BRIGHT,
                0F,
                1F,
                0F);
    }

    private static void ring(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            double scale,
            boolean inner,
            int nearColor,
            int farColor) {
        Vec3 vector = new Vec3(1D, 0D, 0D);

        for (int i = 0; i < RING_COUNT; i++) {
            if (inner) {
                vertex(
                        buffer,
                        pose,
                        vector.x * 0.9D,
                        0D,
                        vector.z * 0.9D,
                        0.5D + vector.x * 0.25D / scale * 0.9D,
                        0.5D + vector.z * 0.25D / scale * 0.9D,
                        BLACK);
                vertex(
                        buffer,
                        pose,
                        vector.x * scale,
                        0D,
                        vector.z * scale,
                        0.5D + vector.x * 0.25D,
                        0.5D + vector.z * 0.25D,
                        nearColor);
                vector = vector.yRot((float) (Math.PI * 2D / RING_COUNT));
                vertex(
                        buffer,
                        pose,
                        vector.x * scale,
                        0D,
                        vector.z * scale,
                        0.5D + vector.x * 0.25D,
                        0.5D + vector.z * 0.25D,
                        nearColor);
                vertex(
                        buffer,
                        pose,
                        vector.x * 0.9D,
                        0D,
                        vector.z * 0.9D,
                        0.5D + vector.x * 0.25D / scale * 0.9D,
                        0.5D + vector.z * 0.25D / scale * 0.9D,
                        BLACK);
            } else {
                vertex(
                        buffer,
                        pose,
                        vector.x * scale,
                        0D,
                        vector.z * scale,
                        0.5D + vector.x * 0.25D,
                        0.5D + vector.z * 0.25D,
                        nearColor);
                vertex(
                        buffer,
                        pose,
                        vector.x * scale * 2D,
                        0D,
                        vector.z * scale * 2D,
                        0.5D + vector.x * 0.5D,
                        0.5D + vector.z * 0.5D,
                        farColor);
                vector = vector.yRot((float) (Math.PI * 2D / RING_COUNT));
                vertex(
                        buffer,
                        pose,
                        vector.x * scale * 2D,
                        0D,
                        vector.z * scale * 2D,
                        0.5D + vector.x * 0.5D,
                        0.5D + vector.z * 0.5D,
                        farColor);
                vertex(
                        buffer,
                        pose,
                        vector.x * scale,
                        0D,
                        vector.z * scale,
                        0.5D + vector.x * 0.25D,
                        0.5D + vector.z * 0.25D,
                        nearColor);
            }
        }
    }

    private static void jets(PoseStack.Pose pose, VertexConsumer buffer) {
        for (int j = -1; j <= 1; j += 2) {
            Vec3 rim = new Vec3(0.5D, 0D, 0D);
            for (int i = 0; i < 12; i++) {
                Vec3 next = rim.yRot((float) (Math.PI / 6D * -j));
                Vertices.emit(buffer, pose, 0F, 0F, 0F, JET_CORE);
                Vertices.emit(buffer, pose, (float) rim.x, 10F * j, (float) rim.z, JET_TIP);
                Vertices.emit(buffer, pose, (float) next.x, 10F * j, (float) next.z, JET_TIP);
                Vertices.emit(buffer, pose, 0F, 0F, 0F, JET_CORE);
                rim = next;
            }
        }
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
    public void extractRenderState(T entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.size = entity.getSize();
        state.age = entity.tickCount + partialTicks;
        state.entityId = entity.getId();
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(state.size, state.size, state.size);
        collector.submitCustomGeometry(
                poseStack,
                HOLE_TYPE,
                (pose, buffer) ->
                        ResourceManager.black_hole.render(
                                pose, buffer, LightCoordsUtil.FULL_BRIGHT, -1));

        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(state.entityId % 90 - 45F));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.entityId % 360));
        if (kind.swirl) {
            submitSwirl(state, poseStack, collector);
        } else {
            submitDisc(state, poseStack, collector);
        }
        if (kind.jets)
            collector.submitCustomGeometry(
                    poseStack, BeamRenderTypes.ADDITIVE, RenderBlackHole::jets);
        poseStack.popPose();
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private void submitSwirl(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-5F * state.age));
        int glow = ARGB.color((int) (kind.glow * 255F), 255, 255, 255);
        for (boolean inner : new boolean[] {true, false}) {
            collector.submitCustomGeometry(
                    poseStack,
                    translucent,
                    (pose, buffer) ->
                            ring(
                                    pose,
                                    buffer,
                                    3D,
                                    inner,
                                    kind.full,
                                    inner ? kind.full : kind.empty));
            collector.submitCustomGeometry(
                    poseStack,
                    additive,
                    (pose, buffer) ->
                            ring(pose, buffer, 3D, inner, glow, inner ? glow : kind.empty));
        }
        poseStack.popPose();
    }

    private void submitDisc(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        for (int k = 0; k < DISC_STEPS; k++) {
            poseStack.pushPose();

            poseStack.mulPose(Axis.YP.rotationDegrees(-state.age * (float) Math.pow(k + 1, 1.25D)));
            double scale = 3D - k * 0.175D;
            int near = kind.discColor(k, 255);
            int far = kind.discColor(k, 0);
            collector.submitCustomGeometry(
                    poseStack,
                    translucent,
                    (pose, buffer) -> ring(pose, buffer, scale, false, near, far));
            collector.submitCustomGeometry(
                    poseStack,
                    additive,
                    (pose, buffer) -> ring(pose, buffer, scale, false, GLOW, far));
            poseStack.popPose();
        }
    }

    public enum Kind {
        HOLE(ResourceManager.black_hole_disc_tex, 0xFFB900, false, true, 0.75F),
        VORTEX(ResourceManager.vortex_tex, 0x3898B3, true, false, 0.75F),
        RAGING(ResourceManager.vortex_tex, 0xE8390D, true, true, 0.25F),
        QUASAR(ResourceManager.quasar_disc_tex, 0xFFB900, false, true, 0.75F);

        final Identifier texture;
        final boolean swirl;
        final boolean jets;
        final float glow;
        final int full;
        final int empty;

        Kind(Identifier texture, int tint, boolean swirl, boolean jets, float glow) {
            this.texture = texture;
            this.swirl = swirl;
            this.jets = jets;
            this.glow = glow;
            this.full = tint | 0xFF000000;
            this.empty = tint & 0x00FFFFFF;
        }

        int discColor(int iteration, int alpha) {
            if (this == QUASAR) {
                int gb = (int) (Math.pow(iteration / 15D, 2D) * 255D);
                return ARGB.color(alpha, 255, gb, gb);
            }
            if (iteration < 5)
                return ARGB.color(alpha, 255, (int) ((0.125F + iteration / 10F) * 255F), 0);
            if (iteration == 5) return ARGB.color(alpha, 255, 255, 0);
            int i = iteration - 6;
            return ARGB.color(
                    alpha,
                    (int) ((1F - i / 9F) * 255F),
                    (int) ((1F - i / 9F) * 255F),
                    (int) (i / 5F * 255F));
        }
    }

    public static final class State extends EntityRenderState {
        float size;
        float age;
        int entityId;
    }
}
