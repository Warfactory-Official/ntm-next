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
import com.hbm.blockentity.door.DoorEasing;
import com.hbm.main.ResourceManager;
import net.minecraft.util.Mth;

public final class SealDoorRenderer implements DoorMotion {
    public static final SealDoorRenderer INSTANCE = new SealDoorRenderer();
    private static final int DOOR = ResourceManager.pheo_seal_door.partId("Door");

    @Override
    public void placeParts(DoorState state, DoorFrame frame) {
        double maxRaise = 1;
        double raise = 0;
        if (state.doorState == DoorBlockEntity.OPEN) raise = maxRaise;

        if (state.animation != null) {
            raise = state.bus("DOOR", 1) * maxRaise;
        }

        float travel = (float) (DoorEasing.hermite((float) Mth.clamp(raise, 0D, maxRaise)) * 0.9D);
        DoorFrame.Part door =
                frame.push()
                        .obj(ResourceManager.pheo_seal_door, DOOR)
                        .texture(ResourceManager.pheo_seal_door_tex)
                        .slide(0F, 0F, travel)
                        .halfspace(0F, 0F, 1F, 0.5001F);
        door.pose.translate(0.5F, 0F, 0F);
    }
}
