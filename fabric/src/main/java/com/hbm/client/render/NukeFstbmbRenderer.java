/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client.render;

import com.hbm.blockentity.bomb.BalefireNukeBlockEntity;
import com.hbm.blocks.machine.HorizontalMachineBlock;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public final class NukeFstbmbRenderer
        implements BlockEntityRenderer<BalefireNukeBlockEntity, NukeFstbmbRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final int BALEFIRE_PART = ResourceManager.fstbmb.partId("Balefire");
    private static final float GLINT_SPEED = 5F;
    private static final float GLINT_COLOR = 0.76F;
    private static final float[] TINT = {0.0F, 0.8F, 0.15F};
    private static final int GLINT_LAYERS = 2;
    private static final float TEXT_SCALE = 0.04F;

    public static float yawFor(Direction facing) {
        return switch (facing) {
            case NORTH -> 90F;
            case WEST -> 180F;
            case SOUTH -> 270F;
            default -> 0F;
        };
    }

    public static void clockPose(PoseStack poseStack) {
        poseStack.translate(0.815D, 0.9275D, 0.5D);
        poseStack.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        poseStack.rotate((Quaternionfc) Axis.YP.rotationDegrees(90F));
        poseStack.translate(0.0D, 1.0D, 0.0D);
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
            BalefireNukeBlockEntity bomb,
            State state,
            float partialTicks,
            Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                bomb, state, partialTicks, camera, breakProgress);
        state.loaded = bomb.loaded;
        state.facing = bomb.getBlockState().getValue(HorizontalMachineBlock.FACING);
        state.clock = bomb.getMinutes() + ":" + bomb.getSeconds();
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (!state.loaded) return;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.rotate((Quaternionfc) Axis.YP.rotationDegrees(yawFor(state.facing)));

        int tint =
                0xFF000000
                        | ((int) (TINT[0] * GLINT_COLOR * 255F) << 16)
                        | ((int) (TINT[1] * GLINT_COLOR * 255F) << 8)
                        | (int) (TINT[2] * GLINT_COLOR * 255F);
        int light = state.lightCoords;
        for (int k = 0; k < GLINT_LAYERS; k++) {
            var type =
                    WeaponRenderTypes.balefireGlint(ResourceManager.glint_bf_tex, k, GLINT_SPEED);
            collector.submitCustomGeometry(
                    poseStack,
                    type,
                    (pose, buffer) ->
                            ResourceManager.fstbmb.renderPart(
                                    pose, buffer, light, tint, BALEFIRE_PART));
        }

        clockPose(poseStack);
        FormattedCharSequence text = Component.literal(state.clock).getVisualOrderText();
        collector.submitText(
                poseStack,
                0,
                0,
                text,
                false,
                Font.DisplayMode.NORMAL,
                LightCoordsUtil.FULL_BRIGHT,
                0xFFFF0000,
                0,
                0);
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        boolean loaded;
        Direction facing = Direction.NORTH;
        String clock = "";
    }
}
