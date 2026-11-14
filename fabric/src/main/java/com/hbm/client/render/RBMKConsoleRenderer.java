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

import com.hbm.blockentity.machine.rbmk.ColumnSnapshot;
import com.hbm.blockentity.machine.rbmk.RBMKConsoleBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
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
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class RBMKConsoleRenderer
        implements BlockEntityRenderer<RBMKConsoleBlockEntity, RBMKConsoleRenderer.State>,
                ConcurrentRenderStateExtraction {
    private final Font font;

    public RBMKConsoleRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.font();
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
    public void extractRenderState(
            RBMKConsoleBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        Direction dir = MultiblockCoreBlock.coreFacing(be.getBlockState());
        state.yaw = Facing.yaw(dir, 90);
        state.columns = be.columns.clone();
        for (int i = 0; i < 6; i++) {
            Component text = screenText(be.screens[i].text);
            state.screenText[i] = text == null ? null : text.getVisualOrderText();
        }
    }

    public static @Nullable Component screenText(@Nullable String display) {
        if (display == null || display.isEmpty()) return null;
        String[] parts = display.split("=");
        return parts.length == 2
                ? Component.translatable(parts[0], parts[1])
                : Component.literal(display);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.rotate((Quaternionfc) Axis.YP.rotationDegrees(state.yaw));
        poseStack.translate(0.5, 0.0, 0.0);

        ColumnSnapshot[] columns = state.columns;
        collector.submitCustomGeometry(
                poseStack,
                RBMKColumnGrid.PANEL,
                (pose, buffer) ->
                        RBMKColumnGrid.emit(
                                pose, buffer, columns, 15, -0.3725F, 3.625F, 0.125F * 7F));
        emitScreens(state, poseStack, collector);

        poseStack.popPose();
    }

    private void emitScreens(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.translate(-0.42, 3.5, 1.75);
        for (int i = 0; i < 6; i++) {
            FormattedCharSequence text = state.screenText[i];
            poseStack.pushPose();
            if (i % 2 == 1) poseStack.translate(0.0, 0.0, 1.75 * -2);
            poseStack.translate(0.0, -0.75 * (i >> 1), 0.0);
            if (text != null) {
                int width = font.width(text);
                float f3 = Math.min(0.03F, 0.8F / Math.max(width, 1));
                poseStack.scale(f3, -f3, f3);
                poseStack.rotate((Quaternionfc) Axis.YP.rotationDegrees(90));
                collector.submitText(
                        poseStack,
                        -width / 2,
                        -font.lineHeight / 2,
                        text,
                        false,
                        Font.DisplayMode.POLYGON_OFFSET,
                        LightCoordsUtil.FULL_BRIGHT,
                        CommonColors.GREEN,
                        0,
                        0);
            }
            poseStack.popPose();
        }
    }

    public static final class State extends BlockEntityRenderState {
        public final FormattedCharSequence[] screenText = new FormattedCharSequence[6];
        public float yaw;
        public ColumnSnapshot[] columns;
    }
}
