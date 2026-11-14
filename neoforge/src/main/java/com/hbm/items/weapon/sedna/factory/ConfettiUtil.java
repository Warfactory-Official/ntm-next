/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.items.weapon.sedna.factory;

import com.hbm.entity.mob.CyberCrabEntity;
import com.hbm.entity.mob.FBIDroneEntity;
import com.hbm.entity.mob.RADBeastEntity;
import com.hbm.entity.mob.TaintCrabEntity;
import com.hbm.entity.mob.TeslaCrabEntity;
import com.hbm.entity.mob.UFOEntity;
import com.hbm.entity.mob.botprime.BOTPrimeBaseEntity;
import com.hbm.lib.ModDamageTypes;
import com.hbm.particle.EffectKinds.GibletType;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.level.Level;

public final class ConfettiUtil {

    private ConfettiUtil() {}

    public static void decideConfetti(LivingEntity entity, DamageSource source) {
        if (entity.isAlive()) return;
        if (source.is(ModDamageTypes.LASER)) pulverize(entity);
        if (source.is(ModDamageTypes.ELECTRIC)) pulverize(entity);
        if (source.is(ModDamageTypes.PLASMA)) cremate(entity);
        if (source.is(DamageTypeTags.IS_EXPLOSION)) gib(entity);
        if (source.is(DamageTypeTags.IS_FIRE)) cremate(entity);
    }

    public static void pulverize(LivingEntity entity) {
        disintegrate(entity, 1F);
    }

    public static void cremate(LivingEntity entity) {
        disintegrate(entity, 0.25F);
    }

    private static void disintegrate(LivingEntity entity, float brightness) {
        Level level = entity.level();
        int amount =
                Mth.clamp(
                        (int)
                                (entity.getBbWidth()
                                        * entity.getBbHeight()
                                        * entity.getBbWidth()
                                        * 25),
                        5,
                        50);
        ParticleCreators.ashes(level, entity, amount, 0.125F);
        ParticleCreators.skeleton(level, entity, brightness);
        EntitySounds.play(
                entity,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                ModSounds.DISINTEGRATION.get(),
                SoundSource.HOSTILE,
                2.0F,
                0.9F + entity.getRandom().nextFloat() * 0.2F);
    }

    public static void gib(LivingEntity entity) {
        if (entity instanceof Ocelot || entity instanceof Cat) return;

        GibletType type = gibType(entity);
        ParticleCreators.skeletonGib(entity.level(), entity, 0.25F);

        if (entity instanceof AbstractSkeleton) return;

        ParticleCreators.giblets(entity.level(), entity, type, 0);
        EntitySounds.play(
                entity,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                SoundSource.HOSTILE,
                2.0F,
                0.95F + entity.getRandom().nextFloat() * 0.2F);
    }

    private static GibletType gibType(LivingEntity entity) {
        if (entity instanceof Slime || entity instanceof MagmaCube || entity instanceof Creeper) {
            return GibletType.SLIME;
        }
        if (entity instanceof AbstractGolem
                || entity instanceof Blaze
                || entity instanceof CyberCrabEntity
                || entity instanceof TeslaCrabEntity
                || entity instanceof TaintCrabEntity
                || entity instanceof FBIDroneEntity
                || entity instanceof RADBeastEntity
                || entity instanceof UFOEntity
                || entity instanceof BOTPrimeBaseEntity) {
            return GibletType.METAL;
        }
        return GibletType.MEAT;
    }
}
