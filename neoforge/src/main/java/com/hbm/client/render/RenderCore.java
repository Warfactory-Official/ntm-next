// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.render.util.RenderSparks;
import com.hbm.render.util.Vertices;
import com.hbm.tileentity.machine.BlockEntityCore;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.Random;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderCore
        implements BlockEntityRenderer<BlockEntityCore, RenderCore.State>,
                ConcurrentRenderStateExtraction {

    private static final int SPARK_PERIOD = 10;
    private static final float FLARE_LENGTH = 5.0F, FLARE_LENGTH_SPREAD = 2.0F;
    private static final float FLARE_WIDTH = 1.0F, FLARE_WIDTH_SPREAD = 1.0F;
    private static final double FLARE_REACH =
            Math.hypot(FLARE_LENGTH + FLARE_LENGTH_SPREAD, FLARE_WIDTH + FLARE_WIDTH_SPREAD);
    private static final int ORB_SHELLS = 16;
    private static final int FLARE_FANS = 150;
    private static final long FLARE_SEED = 432L;
    private static final double PULSE_T = 0.8D;

    private static float pulse(double time, double rate) {
        double ix = (time * rate) % (Math.PI * 2D);
        float p =
                (float)
                        ((1D / PULSE_T)
                                * Math.atan(
                                        (PULSE_T * Math.sin(ix)) / (1 - PULSE_T * Math.cos(ix))));
        return (p + 1F) / 2F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityCore be) {
        return AABB.ofSize(
                Vec3.atCenterOf(be.getBlockPos()),
                2 * FLARE_REACH,
                2 * FLARE_REACH,
                2 * FLARE_REACH);
    }

    @Override
    public void extractRenderState(
            BlockEntityCore be,
            State state,
            float partialTick,
            Vec3 offset,
            ModelFeatureRenderer.@Nullable CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, offset, overlay);
        state.heat = be.heat;
        state.color = be.color;
        state.meltdown = be.meltdownTick;
        state.time = be.getLevel() == null ? 0L : be.getLevel().getGameTime();
        int max = be.tanks[0].getMaxFill() + be.tanks[1].getMaxFill();
        state.fill = max <= 0 ? 0F : (float) (be.tanks[0].getFill() + be.tanks[1].getFill()) / max;
    }

    @Override
    public void submit(
            State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);

        if (state.heat == 0) standby(state, ps, collector);
        else if (state.meltdown) flare(state, ps, collector);
        else orb(state, ps, collector);

        ps.popPose();
    }

    private static void standby(State state, PoseStack ps, SubmitNodeCollector collector) {
        ps.pushPose();
        ps.scale(0.25F, 0.25F, 0.25F);
        collector.submitCustomGeometry(
                ps,
                BeamRenderTypes.ADDITIVE,
                (pose, buf) ->
                        ResourceManager.sphere_uv.render(
                                pose,
                                buf,
                                LightCoordsUtil.FULL_BRIGHT,
                                ARGB.colorFromFloat(1F, 0.5F, 0.5F, 0.5F)));
        ps.scale(1.25F, 1.25F, 1.25F);
        collector.submitCustomGeometry(
                ps,
                BeamRenderTypes.ADDITIVE,
                (pose, buf) ->
                        ResourceManager.sphere_uv.render(
                                pose,
                                buf,
                                LightCoordsUtil.FULL_BRIGHT,
                                ARGB.colorFromFloat(1F, 0.1F, 0.1F, 0.1F)));

        long tenths = state.time / 2L;
        if (tenths % SPARK_PERIOD == 0) {
            collector.submitCustomGeometry(
                    ps,
                    RenderSparks.SPARK_LINES,
                    (pose, buf) -> {
                        for (int i = 0; i < 3; i++) {
                            RenderSparks.renderSpark(
                                    pose,
                                    buf,
                                    (int) tenths + i * 10000,
                                    0,
                                    0,
                                    0,
                                    1.5F,
                                    5,
                                    10,
                                    0xFFFF00,
                                    0xFFFFFF);
                            RenderSparks.renderSpark(
                                    pose,
                                    buf,
                                    (int) (state.time) + i * 10000,
                                    0,
                                    0,
                                    0,
                                    1.5F,
                                    5,
                                    10,
                                    0xFFFF00,
                                    0xFFFFFF);
                        }
                    });
        }
        ps.popPose();
    }

    private static void orb(State state, PoseStack ps, SubmitNodeCollector collector) {
        int tint =
                ARGB.color(
                        255,
                        (int) (((state.color & 0xFF0000) >> 16) * 0.4F),
                        (int) (((state.color & 0x00FF00) >> 8) * 0.4F),
                        (int) ((state.color & 0x0000FF) * 0.4F));

        ps.pushPose();
        float scale = 4.5F * state.fill + 0.5F;
        ps.scale(scale, scale, scale);
        ps.scale(0.25F, 0.25F, 0.25F);

        collector.submitCustomGeometry(
                ps,
                BeamRenderTypes.ADDITIVE,
                (pose, buf) ->
                        ResourceManager.sphere_ruv.render(
                                pose, buf, LightCoordsUtil.FULL_BRIGHT, tint));

        float breath = pulse(state.time, 0.1D);
        for (int i = 0; i <= ORB_SHELLS; i++) {
            float s = 1F + 0.25F * i + (breath * (20 - i)) * 0.125F;
            ps.pushPose();
            ps.scale(s, s, s);
            collector.submitCustomGeometry(
                    ps,
                    BeamRenderTypes.ADDITIVE,
                    (pose, buf) ->
                            ResourceManager.sphere_ruv.render(
                                    pose, buf, LightCoordsUtil.FULL_BRIGHT, tint));
            ps.popPose();
        }
        ps.popPose();
    }

    private static void flare(State state, PoseStack ps, SubmitNodeCollector collector) {
        float r = ((state.color & 0xFF0000) >> 16) / 255F;
        float g = ((state.color & 0x00FF00) >> 8) / 255F;
        float b = (state.color & 0x0000FF) / 255F;
        float spin = state.time / 200.0F;

        ps.pushPose();
        float s = 0.875F + pulse(state.time, 0.2D) * 0.125F;
        ps.scale(s, s, s);

        collector.submitCustomGeometry(
                ps,
                BeamRenderTypes.ADDITIVE,
                (pose, buf) -> {
                    Random random = new Random(FLARE_SEED);
                    PoseStack fan = new PoseStack();
                    fan.last().pose().set(pose.pose());
                    fan.last().normal().set(pose.normal());

                    for (int i = 0; i < FLARE_FANS; i++) {
                        fan.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F));
                        fan.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
                        fan.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0F));
                        fan.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F));
                        fan.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
                        fan.mulPose(
                                Axis.ZP.rotationDegrees(
                                        random.nextFloat() * 360.0F + spin * 90.0F));

                        float len = random.nextFloat() * FLARE_LENGTH_SPREAD + FLARE_LENGTH;
                        float width = random.nextFloat() * FLARE_WIDTH_SPREAD + FLARE_WIDTH;
                        fan.scale(0.999F, 0.999F, 0.999F);
                        emitFan(fan.last(), buf, r, g, b, len, width);
                    }
                });
        ps.popPose();
    }

    private static void emitFan(
            PoseStack.Pose pose,
            VertexConsumer buf,
            float r,
            float g,
            float b,
            float len,
            float width) {
        double[][] skirt = {
            {-0.866D * width, len, -0.5F * width},
            {0.866D * width, len, -0.5F * width},
            {0.0D, len, 1.0F * width},
            {-0.866D * width, len, -0.5F * width}
        };
        int tip = ARGB.colorFromFloat(1F, r, g, b);
        int edge = ARGB.colorFromFloat(0F, r, g, b);
        for (int i = 0; i < skirt.length - 1; i++) {
            Vertices.emit(buf, pose, 0F, 0F, 0F, tip);
            Vertices.emit(
                    buf, pose, (float) skirt[i][0], (float) skirt[i][1], (float) skirt[i][2], edge);
            Vertices.emit(
                    buf,
                    pose,
                    (float) skirt[i + 1][0],
                    (float) skirt[i + 1][1],
                    (float) skirt[i + 1][2],
                    edge);
            Vertices.emit(buf, pose, 0F, 0F, 0F, tip);
        }
    }

    public static final class State extends BlockEntityRenderState {
        public int heat;
        public int color;
        public boolean meltdown;
        public float fill;
        public long time;
    }
}
