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

package com.hbm.blockentity.machine;

import com.hbm.entity.mob.CyberCrabEntity;
import com.hbm.entity.mob.TaintCrabEntity;
import com.hbm.entity.mob.TeslaCrabEntity;
import com.hbm.handler.ArmorUtil;
import com.hbm.lib.ModDamageTypes;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import com.hbm.util.LineOfSight;
import com.hbm.util.SectionAccess;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class TeslaDischarge {
    private static final EquipmentSlot[] ARMOR = {
        EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    private TeslaDischarge() {}

    public static List<Vec3> discharge(
            ServerLevel level,
            double x,
            double y,
            double z,
            double radius,
            @Nullable LivingEntity source) {
        var endpoints = new ArrayList<Vec3>();
        append(level, x, y, z, radius, source, endpoints);
        return endpoints;
    }

    static void append(
            ServerLevel level,
            double x,
            double y,
            double z,
            double radius,
            @Nullable LivingEntity source,
            List<Vec3> endpoints) {
        var candidates =
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(
                                x - radius,
                                y - radius,
                                z - radius,
                                x + radius,
                                y + radius,
                                z + radius),
                        EntitySelector.NO_SPECTATORS);
        var shock = level.damageSources().source(ModDamageTypes.ELECTRIC, null);
        int population = candidates.size();
        var batch = new LineOfSight.Batch(level, new SectionAccess.LoadingReader(level));
        batch.reset(x, y, z);
        int[] slots = new int[population];
        for (int i = 0; i < population; i++) {
            var target = candidates.get(i);
            slots[i] =
                    target == source || target instanceof Ocelot || outside(target, x, y, z, radius)
                            ? -1
                            : batch.add(
                                    target.getX(),
                                    centreY(target),
                                    target.getZ(),
                                    SectionPos.asLong(target.blockPosition()));
        }
        for (int i = 0; i < population; i++) {
            var target = candidates.get(i);
            if (target == source || target instanceof Ocelot || outside(target, x, y, z, radius))
                continue;
            double tx = target.getX();
            double ty = centreY(target);
            double tz = target.getZ();
            if (slots[i] < 0 ? batch.blocked(tx, ty, tz) : batch.blocked(slots[i], tx, ty, tz))
                continue;

            if (target instanceof TaintCrabEntity) {
                target.heal(15F);
                ty = target.getY() + 1.25D;
            } else if (target instanceof TeslaCrabEntity) {
                target.heal(10F);
                ty = target.getY() + 1D;
            } else if (target instanceof CyberCrabEntity) {
            } else if (target instanceof Creeper creeper) {
                creeper.getEntityData().set(Creeper.DATA_IS_POWERED, true);
            } else if (!protectedByArmor(target)) {
                float damage = Mth.clamp(target.getMaxHealth() / 2F, 3F, 20F) / population;
                if (target.hurtServer(level, shock, damage)) {
                    EntitySounds.play(
                            target,
                            tx,
                            target.getY(),
                            tz,
                            ModSounds.TESLA_ZAP.get(),
                            SoundSource.BLOCKS,
                            1F,
                            1F);
                }
            }
            endpoints.add(new Vec3(tx, ty, tz));
        }
    }

    private static double centreY(LivingEntity target) {
        return target.getY() + target.getBbHeight() / 2D;
    }

    private static boolean outside(
            LivingEntity target, double x, double y, double z, double radius) {
        double dx = target.getX() - x;
        double dy = centreY(target) - y;
        double dz = target.getZ() - z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz) > radius;
    }

    private static boolean protectedByArmor(LivingEntity entity) {
        for (var slot : ARMOR) {
            var stack = entity.getItemBySlot(slot);
            if (stack.isEmpty() || !ArmorUtil.isFaradayArmor(stack)) return false;
        }
        return true;
    }
}
