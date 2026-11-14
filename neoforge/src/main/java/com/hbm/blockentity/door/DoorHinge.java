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

package com.hbm.blockentity.door;

public record DoorHinge(
        String part,
        float originX,
        float originY,
        float originZ,
        float swingDegrees,
        float swingFrom,
        float swingTo,
        float liftBlocks,
        float liftFrom,
        float liftTo) {

    public float swingAt(float ticks) {
        return swingDegrees * DoorEasing.hermite(DoorEasing.window(ticks, swingFrom, swingTo));
    }

    public float liftAt(float ticks) {
        return liftBlocks * DoorEasing.hermite(DoorEasing.window(ticks, liftFrom, liftTo));
    }
}
