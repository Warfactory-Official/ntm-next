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

import com.hbm.blockentity.turret.TurretMaxwellBlockEntity;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.BeamPronter.EnumBeamType;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.render.util.BeamPronter;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class TurretMaxwellRenderer extends TurretBaseRenderer<TurretMaxwellBlockEntity> {

    private static final int CARRIAGE = ResourceManager.turret_howard.partId("Carriage");
    private static final int MICROWAVE = ResourceManager.turret_maxwell.partId("Microwave");

    @Override
    protected void emit(TurretMaxwellBlockEntity turret, State state, TurretFrame frame) {
        connectors(frame, turret.connectorMask);
        Matrix4f microwave =
                mount(
                        frame,
                        state,
                        ResourceManager.turret_howard,
                        CARRIAGE,
                        ResourceManager.turret_carriage_ciws_tex,
                        1.5F,
                        ResourceManager.turret_maxwell,
                        MICROWAVE,
                        ResourceManager.turret_maxwell_tex);

        if (turret.beam <= 0) return;
        state.beam = true;
        state.beamLength = turret.lastDist - turret.getBarrelLength();
        state.beamSegments = (int) (turret.lastDist + 1);
        state.effect.set(microwave).translate((float) turret.getBarrelLength(), 2F, 0F);
    }

    @Override
    protected void submitEffects(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (!state.beam) return;

        Vec3 skeleton = new Vec3(state.beamLength, 0D, 0D);
        poseStack.pushPose();
        poseStack.mulPose(state.effect);
        for (int i = 0; i < 8; i++) {
            int start = (int) ((state.gameTime + state.partialTicks) * -50F + i * 45) % 360;
            BeamPronter.prontBeam(
                    poseStack,
                    collector,
                    skeleton,
                    EnumWaveType.SPIRAL,
                    EnumBeamType.SOLID,
                    0x2020ff,
                    0x2020ff,
                    start,
                    state.beamSegments,
                    0.375F,
                    2,
                    0.05F);
        }
        poseStack.popPose();
    }
}
