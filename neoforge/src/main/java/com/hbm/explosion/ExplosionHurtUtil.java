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

package com.hbm.explosion;

import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public final class ExplosionHurtUtil {

    private ExplosionHurtUtil() {}

    public static void doRadiation(
            Level level, double x, double y, double z, float outer, float inner, double radius) {
        List<LivingEntity> entities =
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(
                                x - radius,
                                y - radius,
                                z - radius,
                                x + radius,
                                y + radius,
                                z + radius));

        for (LivingEntity entity : entities) {
            double dx = x - entity.getX();
            double dy = y - entity.getY();
            double dz = z - entity.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist > radius) continue;

            float rad = (float) (outer + (inner - outer) * (1.0 - dist / radius));
            ContaminationUtil.contaminate(
                    entity, HazardType.RADIATION, ContaminationType.CREATIVE, rad);
        }
    }
}
