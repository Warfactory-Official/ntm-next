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

import com.hbm.blockentity.machine.RadarBlockEntity;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class RadarRenderer
        implements BlockEntityRenderer<RadarBlockEntity, RadarRenderer.State>,
                ConcurrentRenderStateExtraction {
    public static final float BASE_YAW = 180F;
    private static final int DISH = ResourceManager.radar.partId("Dish");
    private static final double DISH_OFFSET_X = -0.125D;

    private final HFRWavefrontObject model = ResourceManager.radar;
    private final RenderType dishType =
            WorldRenderPipeline.oneSidedCutout(ResourceManager.radar_dish_tex);

    @Override
    public AABB getRenderBoundingBox(RadarBlockEntity be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 1.0625,
                pos.getY(),
                pos.getZ() - 1.0625,
                pos.getX() + 2.0625,
                pos.getY() + 3,
                pos.getZ() + 2.0625);
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
            RadarBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breaking);
        state.spin = be.prevRotation + (be.rotation - be.prevRotation) * partialTicks;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose((Quaternionfc) Axis.YP.rotationDegrees(BASE_YAW));
        ps.mulPose((Quaternionfc) Axis.YP.rotationDegrees(-s.spin));
        ps.translate(DISH_OFFSET_X, 0.0, 0.0);

        col.submitCustomGeometry(
                ps, dishType, (pose, buf) -> model.renderPart(pose, buf, light, -1, DISH));

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float spin;
    }
}
