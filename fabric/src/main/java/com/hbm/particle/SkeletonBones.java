// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

public final class SkeletonBones {

    private SkeletonBones() {}

    public static boolean isSkeletal(LivingEntity entity) {
        return entity instanceof AbstractSkeleton;
    }

    public static Bone @Nullable [] of(LivingEntity entity) {

        if (entity instanceof Zombie || entity instanceof AbstractSkeleton) return zombie(entity);
        if (entity instanceof AbstractVillager || entity instanceof Witch) return villager(entity);
        if (entity instanceof Player) return biped(entity);
        return null;
    }

    private static Bone[] biped(LivingEntity e) {
        double[] arm = rotate(0.375D, -e.yBodyRot);
        double[] leg = rotate(0.125D, -e.yBodyRot);
        return new Bone[] {
            new Bone(
                    ParticleSkeleton.PART_SKULL,
                    -e.yHeadRot,
                    e.getXRot(),
                    e.getX(),
                    e.getY() + 1.75D,
                    e.getZ()),
            new Bone(
                    ParticleSkeleton.PART_TORSO,
                    -e.yBodyRot,
                    0F,
                    e.getX(),
                    e.getY() + 1.125D,
                    e.getZ()),
            limb(e, arm, 1.125D, 0F, 1),
            limb(e, arm, 1.125D, 0F, -1),
            limb(e, leg, 0.375D, 0F, 1),
            limb(e, leg, 0.375D, 0F, -1),
        };
    }

    private static Bone[] zombie(LivingEntity e) {
        double[] arm = rotate(0.375D, -e.yBodyRot);
        double[] fwd = rotateZ(0.25D, -e.yBodyRot);
        double[] leg = rotate(0.125D, -e.yBodyRot);
        return new Bone[] {
            new Bone(
                    ParticleSkeleton.PART_SKULL,
                    -e.yHeadRot,
                    e.getXRot(),
                    e.getX(),
                    e.getY() + 1.75D,
                    e.getZ()),
            new Bone(
                    ParticleSkeleton.PART_TORSO,
                    -e.yBodyRot,
                    0F,
                    e.getX(),
                    e.getY() + 1.125D,
                    e.getZ()),
            new Bone(
                    ParticleSkeleton.PART_LIMB,
                    -e.yBodyRot,
                    -90F,
                    e.getX() + arm[0] + fwd[0],
                    e.getY() + 1.375D,
                    e.getZ() + arm[1] + fwd[1]),
            new Bone(
                    ParticleSkeleton.PART_LIMB,
                    -e.yBodyRot,
                    -90F,
                    e.getX() - arm[0] + fwd[0],
                    e.getY() + 1.375D,
                    e.getZ() - arm[1] + fwd[1]),
            limb(e, leg, 0.375D, 0F, 1),
            limb(e, leg, 0.375D, 0F, -1),
        };
    }

    private static Bone[] villager(LivingEntity e) {
        double[] arm = rotate(0.375D, -e.yBodyRot);
        double[] fwd = rotateZ(0.25D, -e.yBodyRot);
        double[] leg = rotate(0.125D, -e.yBodyRot);
        return new Bone[] {
            new Bone(
                    ParticleSkeleton.PART_SKULL_VILLAGER,
                    -e.yHeadRot,
                    e.getXRot(),
                    e.getX(),
                    e.getY() + 1.6875D,
                    e.getZ()),
            new Bone(
                    ParticleSkeleton.PART_TORSO,
                    -e.yBodyRot,
                    0F,
                    e.getX(),
                    e.getY() + 1D,
                    e.getZ()),
            new Bone(
                    ParticleSkeleton.PART_LIMB,
                    -e.yBodyRot,
                    -45F,
                    e.getX() + arm[0] + fwd[0],
                    e.getY() + 1.125D,
                    e.getZ() + arm[1] + fwd[1]),
            new Bone(
                    ParticleSkeleton.PART_LIMB,
                    -e.yBodyRot,
                    -45F,
                    e.getX() - arm[0] + fwd[0],
                    e.getY() + 1.125D,
                    e.getZ() - arm[1] + fwd[1]),
            limb(e, leg, 0.375D, 0F, 1),
            limb(e, leg, 0.375D, 0F, -1),
        };
    }

    private static Bone limb(LivingEntity e, double[] off, double dy, float pitch, int sign) {
        return new Bone(
                ParticleSkeleton.PART_LIMB,
                -e.yBodyRot,
                pitch,
                e.getX() + off[0] * sign,
                e.getY() + dy,
                e.getZ() + off[1] * sign);
    }

    private static double[] rotate(double d, float degrees) {
        float rad = degrees * Mth.DEG_TO_RAD;
        return new double[] {d * Mth.cos(rad), -d * Mth.sin(rad)};
    }

    private static double[] rotateZ(double d, float degrees) {
        float rad = degrees * Mth.DEG_TO_RAD;
        return new double[] {d * Mth.sin(rad), d * Mth.cos(rad)};
    }

    public record Bone(int part, float yaw, float pitch, double x, double y, double z) {}
}
