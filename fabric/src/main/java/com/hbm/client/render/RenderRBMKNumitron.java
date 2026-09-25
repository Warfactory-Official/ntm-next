// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.util.Vertices;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKNumitron.DisplayUnit;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKNumitron;
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
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderRBMKNumitron
        implements BlockEntityRenderer<BlockEntityRBMKNumitron, RenderRBMKNumitron.State>,
                ConcurrentRenderStateExtraction {
    public static final int DIGITS = 7;
    private static final float SCALE = 200F;
    private static final float W = 8F / SCALE;
    private static final float H = 13F / SCALE;
    private static final float Y_OFFSET = .5625F;
    private static final float PLANE = .03135F;
    private final Font font;
    private final HFRWavefrontObject model = ResourceManager.rbmk_numitron;
    private final RenderType bodyType =
            RenderTypes.entityCutoutCull(ResourceManager.rbmk_numitron_tex);
    private final RenderType glyphType =
            RenderTypes.entityCutoutCull(ResourceManager.rbmk_numitron_lights_tex);

    public RenderRBMKNumitron(BlockEntityRendererProvider.Context context) {
        font = context.font();
    }

    private static float glyphU(char c) {
        int digit = c - '0';
        if (digit >= 0 && digit <= 9) return .1F * digit;
        return switch (c) {
            case '.' -> .9F;
            case 'k' -> 0F;
            case 'M' -> .1F;
            case 'G' -> .2F;
            case 'T' -> .3F;
            case 'P' -> .4F;
            case 'E' -> .5F;
            default -> .8F;
        };
    }

    private static float glyphV(char c) {
        int digit = c - '0';
        return digit >= 0 && digit <= 9 ? 0F : .5F;
    }

    private static String format(DisplayUnit unit) {
        String value;
        if (unit.shortenNumber) value = BobMathUtil.getShortNumber(unit.value);
        else if (unit.value > 9999999L) value = "9999999";
        else if (unit.value < -999999L) value = "-999999";
        else value = Long.toString(unit.value);
        if (value.length() < DIGITS && value.charAt(0) == '-' && unit.leadingZeroes) {
            value = value.substring(1);
            while (value.length() < DIGITS - 1) value = "0" + value;
            return "-" + value;
        }
        String fill = unit.leadingZeroes ? "0" : " ";
        while (value.length() < DIGITS) value = fill + value;
        return value;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            BlockEntityRBMKNumitron be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = Facing.yaw(be.getBlockState().getValue(RBMKMiniPanelBase.FACING), 90);
        for (int i = 0; i < state.tubes.length; i++) {
            DisplayUnit unit = be.displays[i];
            Tube out = state.tubes[i];
            out.active = unit.active;
            if (!unit.active) continue;
            out.value = format(unit);
            out.activeDigits = unit.activeDigits;
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
        for (int i = 0; i < state.tubes.length; i++) {
            Tube tube = state.tubes[i];
            if (!tube.active) continue;
            poses.pushPose();
            poses.translate(.25, i * -.5 + .25, 0);
            for (int j = 0; j < model.groups.length; j++) {
                int group = j;
                collector.submitCustomGeometry(
                        poses,
                        bodyType,
                        (pose, buffer) -> model.renderPart(pose, buffer, light, -1, group));
            }
            for (int j = 0; j < DIGITS; j++) {
                char c = tube.value.charAt(j);
                boolean present = c != ' ' && (tube.activeDigits & (0x40L >> j)) != 0;
                if (!present) continue;
                float u = glyphU(c), v = glyphV(c);
                poses.pushPose();
                poses.translate(0, 0, -(j - 3) * .1F);
                collector.submitCustomGeometry(
                        poses,
                        glyphType,
                        (pose, buffer) -> {
                            Vertices.emit(
                                    buffer,
                                    pose,
                                    PLANE,
                                    -H + Y_OFFSET,
                                    W,
                                    -1,
                                    u,
                                    v + .5F,
                                    LightCoordsUtil.FULL_BRIGHT,
                                    0F,
                                    1F,
                                    0F);
                            Vertices.emit(
                                    buffer,
                                    pose,
                                    PLANE,
                                    H + Y_OFFSET,
                                    W,
                                    -1,
                                    u,
                                    v,
                                    LightCoordsUtil.FULL_BRIGHT,
                                    0F,
                                    1F,
                                    0F);
                            Vertices.emit(
                                    buffer,
                                    pose,
                                    PLANE,
                                    H + Y_OFFSET,
                                    -W,
                                    -1,
                                    u + .1F,
                                    v,
                                    LightCoordsUtil.FULL_BRIGHT,
                                    0F,
                                    1F,
                                    0F);
                            Vertices.emit(
                                    buffer,
                                    pose,
                                    PLANE,
                                    -H + Y_OFFSET,
                                    -W,
                                    -1,
                                    u + .1F,
                                    v + .5F,
                                    LightCoordsUtil.FULL_BRIGHT,
                                    0F,
                                    1F,
                                    0F);
                        });
                poses.popPose();
            }
            if (tube.label != null) {
                poses.translate(.01, .3125, 0);
                int width = font.width(tube.label);
                float scale = Math.min(.0125F, .75F / Math.max(width, 1));
                poses.scale(scale, -scale, scale);
                poses.mulPose(Axis.YP.rotationDegrees(90F));
                collector.submitText(
                        poses,
                        -width / 2,
                        -font.lineHeight / 2,
                        tube.label,
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

    public static final class Tube {
        public boolean active;
        public String value = "";
        public long activeDigits;
        public @Nullable FormattedCharSequence label;
    }

    public static final class State extends BlockEntityRenderState {
        public final Tube[] tubes = new Tube[BlockEntityRBMKNumitron.DISPLAYS];
        public float yaw;

        public State() {
            for (int i = 0; i < tubes.length; i++) tubes[i] = new Tube();
        }
    }
}
