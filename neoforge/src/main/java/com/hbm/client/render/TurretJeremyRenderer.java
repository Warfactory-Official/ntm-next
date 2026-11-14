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

import com.hbm.blockentity.turret.TurretJeremyBlockEntity;
import com.hbm.main.ResourceManager;

public class TurretJeremyRenderer extends TurretBaseRenderer<TurretJeremyBlockEntity> {

    private static final int CARRIAGE = ResourceManager.turret_chekhov.partId("Carriage");
    private static final int GUN = ResourceManager.turret_jeremy.partId("Gun");

    @Override
    protected void emit(TurretJeremyBlockEntity turret, State state, TurretFrame frame) {
        connectors(frame, turret.connectorMask);
        mount(
                frame,
                state,
                ResourceManager.turret_chekhov,
                CARRIAGE,
                ResourceManager.turret_carriage_tex,
                1.5F,
                ResourceManager.turret_jeremy,
                GUN,
                ResourceManager.turret_jeremy_tex);
    }
}
