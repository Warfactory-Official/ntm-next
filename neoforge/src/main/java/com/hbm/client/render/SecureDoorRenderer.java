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

public final class SecureDoorRenderer implements DoorMotion {
    public static final SecureDoorRenderer INSTANCE = new SecureDoorRenderer();
    private static final int DOOR = ResourceManager.pheo_secure_door.partId("Door");

    @Override
    public void placeParts(DoorState state, DoorFrame frame) {
        double maxRaise = 3.5;
        double raise = 0;
        if (state.doorState == DoorBlockEntity.OPEN) raise = maxRaise;

        if (state.animation != null) {
            raise = state.bus("DOOR", 1) * maxRaise;
        }

        DoorFrame.Part door =
                frame.push()
                        .obj(ResourceManager.pheo_secure_door, DOOR)
                        .texture(state.look.getSkin(state.skinIndex));
        door.pose.translate(0F, 1F, 0F).translate(0F, (float) Mth.clamp(raise, 0D, maxRaise), 0F);
    }
}
