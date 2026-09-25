// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.factory;

import com.hbm.entity.mob.EntityCyberCrab;
import com.hbm.entity.mob.EntityFBIDrone;
import com.hbm.entity.mob.EntityRADBeast;
import com.hbm.entity.mob.EntityTaintCrab;
import com.hbm.entity.mob.EntityTeslaCrab;
import com.hbm.entity.mob.EntityUFO;
import com.hbm.entity.mob.botprime.EntityBOTPrimeBase;
import com.hbm.lib.ModDamageTypes;
import com.hbm.particle.ParticleGiblet;
import com.hbm.particle.helper.ParticleCreators;
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
        level.playSound(
                null,
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

        int type = gibType(entity);
        ParticleCreators.skeletonGib(entity.level(), entity, 0.25F);

        if (entity instanceof AbstractSkeleton) return;

        ParticleCreators.giblets(entity.level(), entity, type, 0);
        entity.level()
                .playSound(
                        null,
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                        SoundSource.HOSTILE,
                        2.0F,
                        0.95F + entity.getRandom().nextFloat() * 0.2F);
    }

    private static int gibType(LivingEntity entity) {
        if (entity instanceof Slime || entity instanceof MagmaCube || entity instanceof Creeper) {
            return ParticleGiblet.TYPE_SLIME;
        }
        if (entity instanceof AbstractGolem
                || entity instanceof Blaze
                || entity instanceof EntityCyberCrab
                || entity instanceof EntityTeslaCrab
                || entity instanceof EntityTaintCrab
                || entity instanceof EntityFBIDrone
                || entity instanceof EntityRADBeast
                || entity instanceof EntityUFO
                || entity instanceof EntityBOTPrimeBase) {
            return ParticleGiblet.TYPE_METAL;
        }
        return ParticleGiblet.TYPE_MEAT;
    }
}
