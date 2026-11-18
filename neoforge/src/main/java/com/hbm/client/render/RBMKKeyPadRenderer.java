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

import com.hbm.blockentity.machine.rbmk.RBMKKeyPadBlockEntity.KeyUnit;
import com.hbm.blockentity.machine.rbmk.RBMKKeyPadBlockEntity;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class RBMKKeyPadRenderer
        implements BlockEntityRenderer<RBMKKeyPadBlockEntity, RBMKKeyPadRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final float PRESS_DEPTH = -.03125F;
    private static final int SOCKET = ResourceManager.rbmk_button.partId("Socket");
    private static final int BUTTON = ResourceManager.rbmk_button.partId("Button");
    private final Font font;
    private final HFRWavefrontObject model = ResourceManager.rbmk_button;
    private final RenderType type = RenderTypes.entityCutoutCull(ResourceManager.rbmk_keypad_tex);
    private final RenderType brightType =
            RenderTypes.entityCutoutCull(ResourceManager.rbmk_keypad_tex);

    public RBMKKeyPadRenderer(BlockEntityRendererProvider.Context context) {
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
    public AABB getRenderBoundingBox(RBMKKeyPadBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(1D);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            RBMKKeyPadBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = Facing.yaw(be.getBlockState().getValue(RBMKMiniPanelBlock.FACING), 90);
        for (int i = 0; i < state.keys.length; i++) {
            KeyUnit unit = be.keys[i];
            Key out = state.keys[i];
            out.active = unit.active;
            if (!unit.active) continue;
            out.pressed = unit.isPressed;
            out.color = dim(unit.color, unit.isPressed ? 1F : .65F);
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
        for (int i = 0; i < state.keys.length; i++) {
            Key key = state.keys[i];
            if (!key.active) continue;
            poses.pushPose();
            poses.translate(.25, (i / 2) * -.5 + .25, (i % 2) * -.5 + .25);
            collector.submitCustomGeometry(
                    poses,
                    type,
                    (pose, buffer) -> model.renderPart(pose, buffer, light, -1, SOCKET));
            poses.pushPose();
            poses.translate(key.pressed ? PRESS_DEPTH : 0, 0, 0);
            int color = key.color;
            collector.submitCustomGeometry(
                    poses,
                    key.pressed ? brightType : type,
                    (pose, buffer) ->
                            model.renderPart(
                                    pose,
                                    buffer,
                                    key.pressed ? LightCoordsUtil.FULL_BRIGHT : light,
                                    color,
                                    BUTTON));
            poses.popPose();
            if (key.label != null) {
                poses.translate(.01, .3125, 0);
                int width = font.width(key.label);
                float scale = Math.min(.0125F, .4F / Math.max(width, 1));
                poses.scale(scale, -scale, scale);
                poses.rotate((Quaternionfc) Axis.YP.rotationDegrees(90F));
                collector.submitText(
                        poses,
                        -width / 2,
                        -font.lineHeight / 2,
                        key.label,
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

    public static final class Key {
        public boolean active;
        public boolean pressed;
        public int color = -1;
        public @Nullable FormattedCharSequence label;
    }

    public static final class State extends BlockEntityRenderState {
        public final Key[] keys = new Key[RBMKKeyPadBlockEntity.KEYS];
        public float yaw;

        public State() {
            for (int i = 0; i < keys.length; i++) keys[i] = new Key();
        }
    }
}
