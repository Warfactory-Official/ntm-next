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

import com.hbm.blockentity.turret.TurretHIMARSBlockEntity;
import com.hbm.items.weapon.AmmoHIMARSItem.HIMARSRocketType;
import com.hbm.main.ResourceManager;
import net.minecraft.util.Mth;

public class TurretHIMARSRenderer extends TurretBaseRenderer<TurretHIMARSBlockEntity> {

    private static final int CARRIAGE = ResourceManager.turret_himars.partId("Carriage");
    private static final int LAUNCHER = ResourceManager.turret_himars.partId("Launcher");
    private static final int CRANE = ResourceManager.turret_himars.partId("Crane");
    private static final int TUBE_STANDARD = ResourceManager.turret_himars.partId("TubeStandard");
    private static final int TUBE_SINGLE = ResourceManager.turret_himars.partId("TubeSingle");
    private static final int CAP_SINGLE = ResourceManager.turret_himars.partId("CapSingle");
    private static final int[] CAPS_STANDARD =
            ResourceManager.turret_himars.partIds(
                    "CapStandard6",
                    "CapStandard5",
                    "CapStandard4",
                    "CapStandard3",
                    "CapStandard2",
                    "CapStandard1");

    @Override
    protected void emit(TurretHIMARSBlockEntity turret, State state, TurretFrame frame) {
        TurretFrame.Part carriage =
                frame.push()
                        .obj(ResourceManager.turret_himars, CARRIAGE)
                        .texture(ResourceManager.turret_himars_tex);
        carriage.pose.rotateY((state.yaw - 180F) * Mth.DEG_TO_RAD);

        TurretFrame.Part launcher =
                frame.push()
                        .obj(ResourceManager.turret_himars, LAUNCHER)
                        .texture(ResourceManager.turret_himars_tex);
        launcher.pose
                .set(carriage.pose)
                .translate(0F, 2.25F, 2F)
                .rotateX(state.pitch * Mth.DEG_TO_RAD)
                .translate(0F, -2.25F, -2F);

        double travel = Mth.lerp(state.partialTicks, turret.lastCrane, turret.crane) * -5D;
        TurretFrame.Part crane =
                frame.push()
                        .obj(ResourceManager.turret_himars, CRANE)
                        .texture(ResourceManager.turret_himars_tex);
        crane.pose.set(launcher.pose).translate(0F, 0F, (float) travel);

        HIMARSRocketType type = turret.loadedType();
        if (type == null) return;

        if (type.modelType == 0) {
            frame.push()
                    .obj(ResourceManager.turret_himars, TUBE_STANDARD)
                    .texture(type.texture)
                    .pose
                    .set(crane.pose);
            for (int i = 0; i < turret.rocketsLeft() && i < CAPS_STANDARD.length; i++) {
                frame.push()
                        .obj(ResourceManager.turret_himars, CAPS_STANDARD[i])
                        .texture(type.texture)
                        .pose
                        .set(crane.pose);
            }
        }

        if (type.modelType == 1) {
            frame.push()
                    .obj(ResourceManager.turret_himars, TUBE_SINGLE)
                    .texture(type.texture)
                    .pose
                    .set(crane.pose);
            if (turret.isLoaded()) {
                frame.push()
                        .obj(ResourceManager.turret_himars, CAP_SINGLE)
                        .texture(type.texture)
                        .pose
                        .set(crane.pose);
            }
        }
    }
}
