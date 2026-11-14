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

import com.hbm.blockentity.machine.TeslaCoilBlockEntity;
import com.hbm.render.util.BeamPronter.EnumBeamType;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.render.util.BeamPronter;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class TeslaRenderer
        implements BlockEntityRenderer<TeslaCoilBlockEntity, TeslaRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final int COLOR = 0x404040;
    private static final float SIZE = 0.125F;
    private static final int LAYERS = 2;
    private static final float THICKNESS = 0.03125F;

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
            TeslaCoilBlockEntity tesla,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                tesla, state, partialTicks, cameraPosition, breakProgress);
        state.beams.clear();
        BlockPos pos = tesla.getBlockPos();
        double sx = pos.getX() + 0.5D;
        double sy = pos.getY() + TeslaCoilBlockEntity.OFFSET;
        double sz = pos.getZ() + 0.5D;
        for (Vec3 target : tesla.arcTargets) state.beams.add(target.subtract(sx, sy, sz));
        state.start = (int) tesla.getLevel().getGameTime() % 1000 + 1;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (state.beams.isEmpty()) return;

        poseStack.pushPose();
        poseStack.translate(0.5D, TeslaCoilBlockEntity.OFFSET, 0.5D);

        for (Vec3 skeleton : state.beams) {
            double length = skeleton.length();
            BeamPronter.prontBeam(
                    poseStack,
                    collector,
                    skeleton,
                    EnumWaveType.RANDOM,
                    EnumBeamType.SOLID,
                    COLOR,
                    COLOR,
                    state.start,
                    (int) (length * 5),
                    SIZE,
                    LAYERS,
                    THICKNESS);
        }

        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public final List<Vec3> beams = new ArrayList<>();
        public int start;
    }
}
