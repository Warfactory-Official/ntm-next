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

import com.hbm.blockentity.machine.rbmk.RBMKLeverBlockEntity.LeverUnit;
import com.hbm.blockentity.machine.rbmk.RBMKLeverBlockEntity;
import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBlock;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
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
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class RBMKLeverRenderer
        implements BlockEntityRenderer<RBMKLeverBlockEntity, RBMKLeverRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final int BASE = ResourceManager.rbmk_lever.partId("Base");
    private static final int LEVER = ResourceManager.rbmk_lever.partId("Lever");
    private final Font font;
    private final HFRWavefrontObject model = ResourceManager.rbmk_lever;
    private final RenderType type = RenderTypes.entityCutoutCull(ResourceManager.rbmk_lever_tex);

    public RBMKLeverRenderer(BlockEntityRendererProvider.Context context) {
        font = context.font();
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
    public void extractRenderState(
            RBMKLeverBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = Facing.yaw(be.getBlockState().getValue(RBMKMiniPanelBlock.FACING), 90);
        for (int i = 0; i < state.throwsAt.length; i++) {
            LeverUnit unit = be.levers[i];
            Throw out = state.throwsAt[i];
            out.active = unit.active;
            if (!unit.active) continue;
            out.progress = Mth.lerp(partialTicks, unit.prevFlipProgress, unit.flipProgress);
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
        for (int i = 0; i < state.throwsAt.length; i++) {
            Throw unit = state.throwsAt[i];
            if (!unit.active) continue;
            poses.pushPose();
            poses.translate(.25, 0, i * -.5 + .25);
            collector.submitCustomGeometry(
                    poses, type, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, BASE));
            poses.pushPose();
            poses.translate(.125, .5625, 0);
            poses.rotate((Quaternionfc) Axis.ZP.rotationDegrees(-180F * unit.progress));
            poses.translate(-.125, -.5625, 0);
            collector.submitCustomGeometry(
                    poses,
                    type,
                    (pose, buffer) -> model.renderPart(pose, buffer, light, -1, LEVER));
            poses.popPose();
            if (unit.label != null) {
                poses.translate(.01, .0625, 0);
                int width = font.width(unit.label);
                float scale = Math.min(.0125F, .4F / Math.max(width, 1));
                poses.scale(scale, -scale, scale);
                poses.rotate((Quaternionfc) Axis.YP.rotationDegrees(90F));
                collector.submitText(
                        poses,
                        -width / 2,
                        -font.lineHeight / 2,
                        unit.label,
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

    public static final class Throw {
        public boolean active;
        public float progress;
        public @Nullable FormattedCharSequence label;
    }

    public static final class State extends BlockEntityRenderState {
        public final Throw[] throwsAt = new Throw[RBMKLeverBlockEntity.LEVERS];
        public float yaw;

        public State() {
            for (int i = 0; i < throwsAt.length; i++) throwsAt[i] = new Throw();
        }
    }
}
