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

import com.hbm.blockentity.door.DoorBlockEntity;
import com.hbm.main.ResourceManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class CargoDoorRenderer implements DoorMotion {
    public static final CargoDoorRenderer INSTANCE = new CargoDoorRenderer();
    private static final int DOOR_TOP = ResourceManager.pheo_cargo_door.partId("DoorTop");
    private static final int DOOR_BOT = ResourceManager.pheo_cargo_door.partId("DoorBot");

    private static void leaf(DoorFrame frame, Identifier texture, int part, float raise) {
        frame.push()
                .obj(ResourceManager.pheo_cargo_door, part)
                .texture(texture)
                .pose
                .translate(0F, raise, 0F);
    }

    @Override
    public void placeParts(DoorState state, DoorFrame frame) {
        double botMove = 0;
        double topMove = 0;
        if (state.doorState == DoorBlockEntity.OPEN) {
            botMove = 2.0;
            topMove = 1.0;
        }

        if (state.animation != null) {
            botMove = Mth.clamp(state.bus("BOT", 1), 0D, 1D) * 2.0;
            topMove = Mth.clamp(state.bus("TOP", 1), 0D, 1D);
        }

        Identifier texture = state.look.getSkin(state.skinIndex);
        leaf(frame, texture, DOOR_TOP, (float) topMove);
        leaf(frame, texture, DOOR_BOT, (float) botMove);
    }
}
