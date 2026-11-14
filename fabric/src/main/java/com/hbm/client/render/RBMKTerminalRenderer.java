/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
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

import com.hbm.blockentity.machine.rbmk.RBMKTerminalBlockEntity;
import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBlock;
import com.hbm.client.gui.RBMKTerminalScreen;
import com.hbm.util.Blink;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class RBMKTerminalRenderer
        implements BlockEntityRenderer<RBMKTerminalBlockEntity, RBMKTerminalRenderer.State>,
                ConcurrentRenderStateExtraction {
    public static final int LINES = 18;

    private static final float SCALE = 1F / 250F;
    private static final String PREFIX = "> ";
    private static final String CARET = "_";
    private static final int MAX_WIDTH = 172;
    private static final int LAYOUT_CACHE_LIMIT = 256;
    private static final int GREEN = ARGB.opaque(0x00ff00);
    private static final int AMBER = ARGB.opaque(0xff8000);
    private static final Quaternionfc TEXT_FACING = Axis.YP.rotationDegrees(90F);

    private final Font font;
    private final Map<String, Line> layouts = new HashMap<>();

    public RBMKTerminalRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.font();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    private Line layout(String label) {
        Line line = layouts.get(label);
        if (line != null) return line;
        if (layouts.size() >= LAYOUT_CACHE_LIMIT) layouts.clear();
        line = fit(label);
        layouts.put(label, line);
        return line;
    }

    private Line fit(String label) {
        int width = font.width(PREFIX);
        int end = 0;
        while (end < label.length()
                && (width += font.width(String.valueOf(label.charAt(end)))) <= MAX_WIDTH) end++;
        String fitted = PREFIX + label.substring(0, end);
        Component text = Component.literal(fitted);
        boolean caretFits = font.width(fitted) + font.width(CARET) <= MAX_WIDTH;
        return new Line(text, caretFits ? Component.literal(fitted + CARET) : text);
    }

    @Override
    public void extractRenderState(
            RBMKTerminalBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = Facing.yaw(be.getBlockState().getValue(RBMKMiniPanelBlock.FACING), 90);
        state.color = be.doesRepeat ? AMBER : GREEN;

        state.caret = RBMKTerminalScreen.isEditing(be) && Blink.lit();
        for (int i = 0; i < LINES; i++) {
            String label = i == 0 ? RBMKTerminalScreen.getWorkingLine(be) : be.history[i - 1];
            state.labels[i] = label == null ? "" : label;
        }
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose((Quaternionfc) Axis.YP.rotationDegrees(state.yaw));
        poseStack.translate(0.25, 0.0, 0.0);
        poseStack.translate(0.0635, 0.125, 0.0625 * 5.5);

        for (int i = 0; i < LINES; i++) {
            poseStack.translate(0.0, 10 * SCALE, 0.0);
            String label = state.labels[i];
            if (i > 0 && label.isEmpty()) continue;
            Line line = layout(label);

            poseStack.pushPose();
            poseStack.scale(SCALE, -SCALE, SCALE);
            poseStack.mulPose(TEXT_FACING);
            collector.submitText(
                    poseStack,
                    0,
                    -font.lineHeight / 2,
                    (i == 0 && state.caret ? line.withCaret() : line.text()).getVisualOrderText(),
                    false,
                    Font.DisplayMode.NORMAL,
                    LightCoordsUtil.FULL_BRIGHT,
                    state.color,
                    0,
                    0);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private record Line(Component text, Component withCaret) {}

    public static final class State extends BlockEntityRenderState {
        public final String[] labels = new String[LINES];
        public boolean caret;
        public float yaw;
        public int color = GREEN;
    }
}
