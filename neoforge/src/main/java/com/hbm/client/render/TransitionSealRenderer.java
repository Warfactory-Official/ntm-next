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

public final class TransitionSealRenderer implements DoorMotion {

    public static final TransitionSealRenderer INSTANCE = new TransitionSealRenderer();

    @Override
    public boolean blended() {
        return true;
    }

    @Override
    public void placeParts(DoorState state, DoorFrame frame) {
        long time = state.animMillis;

        long anchor = state.doorState > DoorBlockEntity.OPEN ? state.animStartTime : time;
        boolean reverse =
                state.doorState == DoorBlockEntity.OPEN
                        || state.doorState == DoorBlockEntity.SHUTTING;

        state.walkArmature(
                ResourceManager.transition_seal(),
                ResourceManager.transition_seal_anim(),
                time,
                anchor,
                reverse,
                ResourceManager.transition_seal_tex,
                0F,
                0F,
                0.5F);
    }
}
