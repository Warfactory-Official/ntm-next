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

import com.hbm.blockentity.turret.TurretChekhovBlockEntity;
import com.hbm.main.ResourceManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class TurretChekhovRenderer<T extends TurretChekhovBlockEntity>
        extends TurretBaseRenderer<T> {

    private static final int CARRIAGE = ResourceManager.turret_chekhov.partId("Carriage");
    private static final int BODY = ResourceManager.turret_chekhov.partId("Body");
    private static final int BARRELS = ResourceManager.turret_chekhov.partId("Barrels");

    private final Identifier carriageTex;

    public TurretChekhovRenderer(Identifier carriageTex) {
        this.carriageTex = carriageTex;
    }

    @Override
    protected void emit(T turret, State state, TurretFrame frame) {
        connectors(frame, turret.connectorMask);
        Matrix4f body =
                mount(
                        frame,
                        state,
                        ResourceManager.turret_chekhov,
                        CARRIAGE,
                        carriageTex,
                        1.5F,
                        ResourceManager.turret_chekhov,
                        BODY,
                        ResourceManager.turret_chekhov_tex);

        float spin = Mth.lerp(state.partialTicks, turret.lastSpin, turret.spin);
        TurretFrame.Part barrels =
                frame.push()
                        .obj(ResourceManager.turret_chekhov, BARRELS)
                        .texture(ResourceManager.turret_chekhov_barrels_tex);
        barrels.pose
                .set(body)
                .translate(0F, 1.5F, 0F)
                .rotateX(-spin * Mth.DEG_TO_RAD)
                .translate(0F, -1.5F, 0F);
    }
}
