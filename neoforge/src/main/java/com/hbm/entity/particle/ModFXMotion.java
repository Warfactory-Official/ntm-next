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

package com.hbm.entity.particle;

import net.minecraft.world.phys.Vec3;

final class ModFXMotion {

    private ModFXMotion() {}

    static Vec3 spawn(double motionX, double motionY, double motionZ) {

        double x = (float) (Math.random() * 2.0D - 1.0D) * 0.4F;
        double y = (float) (Math.random() * 2.0D - 1.0D) * 0.4F;
        double z = (float) (Math.random() * 2.0D - 1.0D) * 0.4F;

        float speed = (float) (Math.random() + Math.random() + 1.0D) * 0.15F;
        float length = (float) Math.sqrt(x * x + y * y + z * z);

        x = x / length * speed * 0.4000000059604645D;
        y = y / length * speed * 0.4000000059604645D + 0.10000000149011612D;
        z = z / length * speed * 0.4000000059604645D;

        return new Vec3(
                x * 0.10000000149011612D + motionX,
                y * 0.10000000149011612D + motionY,
                z * 0.10000000149011612D + motionZ);
    }
}
