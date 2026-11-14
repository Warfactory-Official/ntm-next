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

import com.hbm.blockentity.turret.TurretSentryBlockEntity;
import com.hbm.blockentity.turret.TurretSentryDamagedBlockEntity;
import com.hbm.main.ResourceManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class TurretSentryRenderer extends TurretBaseRenderer<TurretSentryBlockEntity> {

    private static final int PIVOT = ResourceManager.turret_sentry.partId("Pivot");
    private static final int BODY = ResourceManager.turret_sentry.partId("Body");
    private static final int DRUM = ResourceManager.turret_sentry.partId("Drum");
    private static final int BARREL_L = ResourceManager.turret_sentry.partId("BarrelL");
    private static final int BARREL_R = ResourceManager.turret_sentry.partId("BarrelR");

    @Override
    protected void emit(TurretSentryBlockEntity turret, State state, TurretFrame frame) {
        boolean damaged = turret instanceof TurretSentryDamagedBlockEntity;
        Identifier tex =
                damaged
                        ? ResourceManager.turret_sentry_damaged_tex
                        : ResourceManager.turret_sentry_tex;

        TurretFrame.Part pivot =
                frame.push().obj(ResourceManager.turret_sentry, PIVOT).texture(tex);
        pivot.pose.rotateY(state.yaw * Mth.DEG_TO_RAD);

        TurretFrame.Part body = frame.push().obj(ResourceManager.turret_sentry, BODY).texture(tex);
        body.pose
                .set(pivot.pose)
                .translate(0F, 1.25F, 0F)
                .rotateX(-state.pitch * Mth.DEG_TO_RAD)
                .translate(0F, -1.25F, 0F);

        frame.push().obj(ResourceManager.turret_sentry, DRUM).texture(tex).pose.set(body.pose);

        double left =
                Mth.lerp(state.partialTicks, turret.lastBarrelLeftPos, turret.barrelLeftPos)
                        * -0.5D;
        frame.push()
                .obj(ResourceManager.turret_sentry, BARREL_L)
                .texture(tex)
                .pose
                .set(body.pose)
                .translate(0F, 0F, (float) left);

        TurretFrame.Part right =
                frame.push().obj(ResourceManager.turret_sentry, BARREL_R).texture(tex);
        right.pose.set(body.pose);
        if (damaged) {
            right.pose
                    .translate(0F, 1.5F, 0.5F)
                    .rotateX(25F * Mth.DEG_TO_RAD)
                    .translate(0F, -1.5F, -0.5F);
        } else {
            double recoil =
                    Mth.lerp(state.partialTicks, turret.lastBarrelRightPos, turret.barrelRightPos)
                            * -0.5D;
            right.pose.translate(0F, 0F, (float) recoil);
        }
    }
}
