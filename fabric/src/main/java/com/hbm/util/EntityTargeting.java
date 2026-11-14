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

package com.hbm.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class EntityTargeting {
    private EntityTargeting() {}

    public static HitResult getMouseOver(Player attacker, double reach) {
        return getMouseOver(attacker, reach, 0D);
    }

    public static HitResult getMouseOver(Player attacker, double reach, double threshold) {
        Vec3 from = attacker.getEyePosition();
        HitResult blockHit = attacker.pick(reach, 1F, false);
        double maxDistanceSqr = reach * reach;
        double blockDistanceSqr = Double.MAX_VALUE;
        double rayLength = reach;
        if (blockHit.getType() != HitResult.Type.MISS) {
            blockDistanceSqr = blockHit.getLocation().distanceToSqr(from);
            maxDistanceSqr = blockDistanceSqr;
            rayLength = Math.sqrt(blockDistanceSqr);
        }

        Vec3 look = attacker.getViewVector(1F);
        Vec3 to = from.add(look.scale(rayLength));
        AABB search = attacker.getBoundingBox().expandTowards(look.scale(rayLength)).inflate(1D);
        EntityHitResult entityHit =
                threshold == 0D
                        ? ProjectileUtil.getEntityHitResult(
                                attacker,
                                from,
                                to,
                                search,
                                EntitySelector.CAN_BE_PICKED,
                                maxDistanceSqr)
                        : getEntityHitResult(attacker, from, to, search, threshold, maxDistanceSqr);
        if (entityHit != null && entityHit.getLocation().distanceToSqr(from) < blockDistanceSqr)
            return entityHit;
        return blockHit.getType() == HitResult.Type.MISS ? null : blockHit;
    }

    private static EntityHitResult getEntityHitResult(
            Player attacker,
            Vec3 from,
            Vec3 to,
            AABB search,
            double threshold,
            double maxDistanceSqr) {
        double nearest = maxDistanceSqr;
        Entity hovered = null;
        Vec3 hoveredPos = null;
        for (Entity entity :
                attacker.level().getEntities(attacker, search, EntitySelector.CAN_BE_PICKED)) {
            AABB box = entity.getBoundingBox().inflate(entity.getPickRadius() + threshold);
            Vec3 clip = box.clip(from, to).orElse(null);
            if (box.contains(from)) {
                if (nearest >= 0D && entity.canBePickedFromInside()) {
                    hovered = entity;
                    hoveredPos = clip == null ? from : clip;
                    nearest = 0D;
                }
            } else if (clip != null) {
                double distanceSqr = from.distanceToSqr(clip);
                if (distanceSqr < nearest || nearest == 0D) {
                    if (entity.getRootVehicle() == attacker.getRootVehicle()) {
                        if (nearest == 0D) {
                            hovered = entity;
                            hoveredPos = clip;
                        }
                    } else {
                        hovered = entity;
                        hoveredPos = clip;
                        nearest = distanceSqr;
                    }
                }
            }
        }
        return hovered == null ? null : new EntityHitResult(hovered, hoveredPos);
    }
}
