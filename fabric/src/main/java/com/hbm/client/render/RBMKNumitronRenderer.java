/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.client.render;

import com.hbm.blockentity.machine.rbmk.RBMKNumitronBlockEntity.DisplayUnit;
import com.hbm.blockentity.machine.rbmk.RBMKNumitronBlockEntity;
import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBlock;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.util.Vertices;
import com.hbm.util.Facing;
import com.hbm.util.NumberFormatter;
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
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class RBMKNumitronRenderer
        implements BlockEntityRenderer<RBMKNumitronBlockEntity, RBMKNumitronRenderer.State>,
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

    public RBMKNumitronRenderer(BlockEntityRendererProvider.Context context) {
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
        if (unit.shortenNumber) value = NumberFormatter.formatCompact(unit.value);
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
            RBMKNumitronBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = Facing.yaw(be.getBlockState().getValue(RBMKMiniPanelBlock.FACING), 90);
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
        poses.rotate((Quaternionfc) Axis.YP.rotationDegrees(state.yaw));
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
                poses.rotate((Quaternionfc) Axis.YP.rotationDegrees(90F));
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
        public final Tube[] tubes = new Tube[RBMKNumitronBlockEntity.DISPLAYS];
        public float yaw;

        public State() {
            for (int i = 0; i < tubes.length; i++) tubes[i] = new Tube();
        }
    }
}
