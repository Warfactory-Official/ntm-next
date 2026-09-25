// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKIndicator.IndicatorUnit;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKIndicator;
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
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderRBMKIndicator
        implements BlockEntityRenderer<BlockEntityRBMKIndicator, RenderRBMKIndicator.State>,
                ConcurrentRenderStateExtraction {
    private static final float DIM = .35F;
    private static final int BASE = ResourceManager.rbmk_indicator.partId("Base");
    private static final int LIGHT = ResourceManager.rbmk_indicator.partId("Light");
    private final Font font;
    private final HFRWavefrontObject model = ResourceManager.rbmk_indicator;
    private final RenderType type =
            RenderTypes.entityCutoutCull(ResourceManager.rbmk_indicator_tex);
    private final RenderType brightType =
            RenderTypes.entityCutoutCull(ResourceManager.rbmk_indicator_tex);

    public RenderRBMKIndicator(BlockEntityRendererProvider.Context context) {
        font = context.font();
    }

    private static int dim(int color, float mult) {
        return ARGB.colorFromFloat(
                1F,
                ((color >> 16) & 0xFF) / 255F * mult,
                ((color >> 8) & 0xFF) / 255F * mult,
                (color & 0xFF) / 255F * mult);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityRBMKIndicator be) {
        return new AABB(be.getBlockPos()).inflate(1D);
    }

    @Override
    public void extractRenderState(
            BlockEntityRBMKIndicator be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = Facing.yaw(be.getBlockState().getValue(RBMKMiniPanelBase.FACING), 90);
        for (int i = 0; i < state.lamps.length; i++) {
            IndicatorUnit unit = be.indicators[i];
            Lamp out = state.lamps[i];
            out.active = unit.active;
            if (!unit.active) continue;
            out.light = unit.light;
            out.color = dim(unit.color, unit.light ? 1F : DIM);
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
        for (int i = 0; i < state.lamps.length; i++) {
            Lamp lamp = state.lamps[i];
            if (!lamp.active) continue;
            poses.pushPose();
            poses.translate(.25, (i / 2) * -.3125 + .3125, (i % 2) * -.5 + .25);
            collector.submitCustomGeometry(
                    poses, type, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, BASE));
            int color = lamp.color;
            if (lamp.light) {
                collector.submitCustomGeometry(
                        poses,
                        brightType,
                        (pose, buffer) ->
                                model.renderPart(
                                        pose,
                                        buffer,
                                        LightCoordsUtil.FULL_BRIGHT,
                                        0xFF000000 | color,
                                        LIGHT));
            } else {
                collector.submitCustomGeometry(
                        poses,
                        type,
                        (pose, buffer) -> model.renderPart(pose, buffer, light, color, LIGHT));
            }
            if (lamp.label != null) {
                poses.translate(.0725, .5, 0);
                int width = font.width(lamp.label);
                float scale = Math.min(.0125F, .3F / Math.max(width, 1));
                poses.scale(scale, -scale, scale);
                poses.mulPose(Axis.YP.rotationDegrees(90F));
                collector.submitText(
                        poses,
                        -width / 2,
                        -font.lineHeight / 2,
                        lamp.label,
                        false,
                        Font.DisplayMode.NORMAL,
                        light,
                        ARGB.opaque(0),
                        0,
                        0);
            }
            poses.popPose();
        }
        poses.popPose();
    }

    public static final class Lamp {
        public boolean active;
        public boolean light;
        public int color = -1;
        public @Nullable FormattedCharSequence label;
    }

    public static final class State extends BlockEntityRenderState {
        public final Lamp[] lamps = new Lamp[BlockEntityRBMKIndicator.INDICATORS];
        public float yaw;

        public State() {
            for (int i = 0; i < lamps.length; i++) lamps[i] = new Lamp();
        }
    }
}
