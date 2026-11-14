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

import com.hbm.blockentity.turret.TurretArtyBlockEntity;
import com.hbm.main.ResourceManager;
import net.minecraft.util.Mth;

public class TurretArtyRenderer extends TurretBaseRenderer<TurretArtyBlockEntity> {

    private static final int CARRIAGE = ResourceManager.turret_arty.partId("Carriage");
    private static final int CANNON = ResourceManager.turret_arty.partId("Cannon");
    private static final int BARREL = ResourceManager.turret_arty.partId("Barrel");

    @Override
    protected void emit(TurretArtyBlockEntity turret, State state, TurretFrame frame) {
        TurretFrame.Part carriage =
                frame.push()
                        .obj(ResourceManager.turret_arty, CARRIAGE)
                        .texture(ResourceManager.turret_arty_tex);
        carriage.pose.rotateY((state.yaw - 180F) * Mth.DEG_TO_RAD);

        TurretFrame.Part cannon =
                frame.push()
                        .obj(ResourceManager.turret_arty, CANNON)
                        .texture(ResourceManager.turret_arty_tex);
        cannon.pose
                .set(carriage.pose)
                .translate(0F, 3F, 0F)
                .rotateX(state.pitch * Mth.DEG_TO_RAD)
                .translate(0F, -3F, 0F);

        double barrel = Mth.lerp(state.partialTicks, turret.lastBarrelPos, turret.barrelPos) * 2.5D;
        frame.push()
                .obj(ResourceManager.turret_arty, BARREL)
                .texture(ResourceManager.turret_arty_tex)
                .pose
                .set(cannon.pose)
                .translate(0F, 0F, (float) barrel);
    }
}
