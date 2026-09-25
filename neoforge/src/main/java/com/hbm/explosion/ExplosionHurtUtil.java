// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
