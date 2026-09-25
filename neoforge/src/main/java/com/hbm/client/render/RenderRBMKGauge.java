// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKGauge.GaugeUnit;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKGauge;
import com.hbm.util.BobMathUtil;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderRBMKGauge
        implements BlockEntityRenderer<BlockEntityRBMKGauge, RenderRBMKGauge.State>,
                ConcurrentRenderStateExtraction {
    public static final float LINE_SCALE = .0025F;
    private final Font font;
    private final HFRWavefrontObject model = ResourceManager.rbmk_gauge;
    private final RenderType bodyType =
            RenderTypes.entityCutoutCull(ResourceManager.rbmk_gauge_tex);
    private final RenderType needleType = RenderTypes.entityCutoutCull(ResourceManager.white_tex);

    public RenderRBMKGauge(BlockEntityRendererProvider.Context context) {
        font = context.font();
    }

    public static String bound(long bound) {
        return Math.abs(bound) <= 10_000 ? bound + "" : BobMathUtil.getShortNumber(bound);
    }

    private static FormattedCharSequence line(long bound) {
        return Component.literal(bound(bound)).getVisualOrderText();
    }

    private static float yaw(Direction facing) {
        return Facing.yaw(facing, 90);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityRBMKGauge be) {
        return new AABB(be.getBlockPos()).inflate(1D);
    }

    @Override
    public void extractRenderState(
            BlockEntityRBMKGauge be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = yaw(be.getBlockState().getValue(RBMKMiniPanelBase.FACING));
        for (int i = 0; i < state.gauges.length; i++) {
            GaugeUnit unit = be.gauges[i];
            Gauge out = state.gauges[i];
            out.active = unit.active;
            if (!unit.active) continue;
            out.color = 0xFF000000 | unit.color;
            long lower = Math.min(unit.min, unit.max);
            long upper = Math.max(unit.min, unit.max);
            if (lower == upper) upper++;
            double value =
                    unit.lastRenderValue + (unit.renderValue - unit.lastRenderValue) * partialTicks;
            double angle = (value - lower) / (double) (upper - lower) * 50D;
            if (unit.min > unit.max) angle = 50D - angle;
            out.angle = (float) Mth.clamp(angle, 0D, 80D);
            out.lower = line(unit.min);
            out.upper = line(unit.max);
            out.label =
                    unit.label == null || unit.label.isEmpty()
                            ? null
                            : Component.literal(unit.label).getVisualOrderText();
        }
    }

    @Override
    public void submit(
            State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        poses.pushPose();
        poses.translate(.5, 0, .5);
        poses.mulPose(Axis.YP.rotationDegrees(state.yaw));
        int light = state.lightCoords;
        for (int i = 0; i < state.gauges.length; i++) {
            Gauge gauge = state.gauges[i];
            if (!gauge.active) continue;
            poses.pushPose();
            poses.translate(.25, (i / 2) * -.5 + .25, (i % 2) * -.5 + .25);
            collector.submitCustomGeometry(
                    poses,
                    bodyType,
                    (pose, buffer) ->
                            model.renderPart(pose, buffer, light, -1, model.partId("Gauge")));
            poses.pushPose();
            poses.translate(0, .4375, -.125);
            poses.mulPose(Axis.XN.rotationDegrees(gauge.angle - 85F));
            poses.translate(0, -.4375, .125);
            int color = gauge.color;
            collector.submitCustomGeometry(
                    poses,
                    needleType,
                    (pose, buffer) ->
                            model.renderPart(
                                    pose,
                                    buffer,
                                    LightCoordsUtil.FULL_BRIGHT,
                                    color,
                                    model.partId("Needle")));
            poses.popPose();
            int height = font.lineHeight;
            for (int j = 0; j < 2; j++) {
                poses.pushPose();
                poses.translate(0, .4375, -.125);
                poses.mulPose(Axis.XN.rotationDegrees(10F + j * 50F));
                poses.translate(0, -.4375, .125);
                poses.translate(.032, .4375, .125);
                poses.scale(LINE_SCALE, -LINE_SCALE, LINE_SCALE);
                poses.mulPose(Axis.YP.rotationDegrees(90F));
                collector.submitText(
                        poses,
                        0,
                        -height / 2,
                        j == 0 ? gauge.lower : gauge.upper,
                        false,
                        Font.DisplayMode.NORMAL,
                        light,
                        ARGB.opaque(0),
                        0,
                        0);
                poses.popPose();
            }
            if (gauge.label != null) {
                poses.translate(.01, .3125, 0);
                int width = font.width(gauge.label);
                float scale = Math.min(.0125F, .4F / Math.max(width, 1));
                poses.scale(scale, -scale, scale);
                poses.mulPose(Axis.YP.rotationDegrees(90F));
                collector.submitText(
                        poses,
                        -width / 2,
                        -height / 2,
                        gauge.label,
                        false,
                        Font.DisplayMode.NORMAL,
                        LightCoordsUtil.FULL_BRIGHT,
                        ARGB.opaque(0x00FF00),
                        0,
                        0);
            }
            poses.popPose();
        }
        poses.popPose();
    }

    public static final class Gauge {
        public boolean active;
        public int color = -1;
        public float angle;
        public FormattedCharSequence lower = FormattedCharSequence.EMPTY;
        public FormattedCharSequence upper = FormattedCharSequence.EMPTY;
        public @Nullable FormattedCharSequence label;
    }

    public static final class State extends BlockEntityRenderState {
        public final Gauge[] gauges = new Gauge[BlockEntityRBMKGauge.GAUGES];
        public float yaw;

        public State() {
            for (int i = 0; i < gauges.length; i++) gauges[i] = new Gauge();
        }
    }
}
