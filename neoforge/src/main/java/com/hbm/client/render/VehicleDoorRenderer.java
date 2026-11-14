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
import net.minecraft.util.Mth;

public final class VehicleDoorRenderer implements DoorMotion {
    public static final VehicleDoorRenderer INSTANCE = new VehicleDoorRenderer();
    private static final int LEFT = ResourceManager.pheo_vehicle_door.partId("Left");
    private static final int RIGHT = ResourceManager.pheo_vehicle_door.partId("Right");

    private static void leaf(DoorFrame frame, int part, float travel) {
        DoorFrame.Part leaf =
                frame.push()
                        .obj(ResourceManager.pheo_vehicle_door, part)
                        .texture(ResourceManager.pheo_vehicle_door_tex)
                        .slide(travel, 0F, 0F)
                        .slab(1F, 0F, 0F, 3.4375F);
        leaf.pose.rotateY(DoorMotion.radians(90));
    }

    @Override
    public boolean backfaceCulled() {
        return false;
    }

    @Override
    public void placeParts(DoorState state, DoorFrame frame) {
        double maxOpen = 3;
        double open = 0;
        if (state.doorState == DoorBlockEntity.OPEN) open = maxOpen;

        if (state.animation != null) {
            open = state.bus("DOOR", 1) * maxOpen;
        }

        float travel = (float) Mth.clamp(open, 0D, maxOpen);
        leaf(frame, LEFT, -travel);
        leaf(frame, RIGHT, travel);
    }
}
