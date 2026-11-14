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

public final class VaultDoorRenderer implements DoorMotion {
    public static final VaultDoorRenderer INSTANCE = new VaultDoorRenderer();
    private static final int DOOR = ResourceManager.pheo_vault_door.partId("Door");
    private static final int LABEL = ResourceManager.pheo_vault_door.partId("Label");

    @Override
    public void placeParts(DoorState state, DoorFrame frame) {
        double pull = 0;
        double slide = 0;
        if (state.doorState == DoorBlockEntity.OPEN) {
            pull = 1;
            slide = 1;
        }

        if (state.animation != null) {
            pull = state.bus("PULL", 2);
            slide = state.bus("SLIDE", 0);
        }

        double diameter = 4.25D;
        double circumference = diameter * Math.PI;
        slide *= 5D;
        double roll = 360D * slide / circumference;

        DoorFrame.Part door =
                frame.push()
                        .obj(ResourceManager.pheo_vault_door, DOOR)
                        .texture(state.look.getTexture(state.skinIndex, "Door"));
        door.pose
                .translate((float) -pull, 0F, 0F)
                .translate(0F, 0F, (float) slide)
                .translate(0F, 2.5F, 0F)
                .rotateX(DoorMotion.radians(roll))
                .translate(0F, -2.5F, 0F);

        frame.push()
                .obj(ResourceManager.pheo_vault_door, LABEL)
                .texture(state.look.getTexture(state.skinIndex, "Label"))
                .pose
                .set(door.pose);
    }
}
