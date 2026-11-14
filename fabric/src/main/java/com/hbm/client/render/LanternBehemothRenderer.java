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

import com.hbm.blockentity.prop.BehemothLanternBlockEntity;
import com.hbm.blocks.generic.LanternBehemothBlock;
import com.hbm.main.ResourceManager;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public final class LanternBehemothRenderer
        implements BlockEntityRenderer<BehemothLanternBlockEntity, LanternBehemothRenderer.State>,
                ConcurrentRenderStateExtraction {

    private static final int LANTERN = ResourceManager.lantern.partId("Lantern");
    private static final int LIGHT = ResourceManager.lantern.partId("Light");
    private static final RenderType BODY =
            WorldRenderPipeline.oneSidedCutout(ResourceManager.lantern_rusty_tex);
    private static final RenderType LIGHT_TYPE = FlatCutout.of(ResourceManager.white_tex);

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
            BehemothLanternBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breaking);
        state.broken = be.getBlockState().getValue(LanternBehemothBlock.BROKEN);
        float wave = (float) (Math.sin(GameTime.now() / 200.0D) * 0.5D + 0.5D);
        state.color =
                state.broken
                        ? ARGB.colorFromFloat(1.0F, wave, 0.0F, 0.0F)
                        : ARGB.colorFromFloat(1.0F, 0.0F, wave * 0.5F + 0.5F, 0.0F);
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5D, 0.0D, 0.5D);
        if (state.broken) {
            pose.mulPose((Quaternionfc) Axis.XP.rotationDegrees(5.0F));
            pose.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(10.0F));
            collector.submitCustomGeometry(
                    pose,
                    BODY,
                    (p, buffer) ->
                            ResourceManager.lantern.renderPart(
                                    p, buffer, state.lightCoords, -1, LANTERN));
        }
        collector.submitCustomGeometry(
                pose,
                LIGHT_TYPE,
                (p, buffer) ->
                        ResourceManager.lantern.renderPart(
                                p, buffer, LightCoordsUtil.FULL_BRIGHT, state.color, LIGHT));
        pose.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        private boolean broken;
        private int color;
    }
}
