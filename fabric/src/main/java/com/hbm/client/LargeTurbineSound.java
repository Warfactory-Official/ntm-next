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

package com.hbm.client;

import com.hbm.blockentity.machine.LargeTurbineBlockEntity;

public final class LargeTurbineSound {

    private LargeTurbineSound() {}

    public static void tick(LargeTurbineBlockEntity be) {
        float speed = be.fanAcceleration / 15F;
        be.audioLoop(
                be.operational || be.fanAcceleration > 0F, 0.4F * speed, 0.25F + 0.75F * speed);
    }
}
