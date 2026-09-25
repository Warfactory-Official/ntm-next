// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class EntityAIMaskmanCasualApproach extends MeleeAttackGoal {

    private final double speed;
    private Path approachPath;
    private int repathTicks;
    private double lastX;
    private double lastY;
    private double lastZ;

    public EntityAIMaskmanCasualApproach(PathfinderMob attacker, double speed, boolean longMemory) {
        super(attacker, speed, longMemory);
        this.speed = speed;
    }

    @Override
    public boolean canUse() {
        if (!super.canUse()) return false;
        LivingEntity target = mob.getTarget();
        if (target == null) return false;
        Vec3 destination = approachPos(target);
        this.approachPath =
                mob.getNavigation().createPath(destination.x, destination.y, destination.z, 0);
        return approachPath != null;
    }

    @Override
    public void start() {
        super.start();
        mob.getNavigation().moveTo(approachPath, speed);
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;

        mob.getLookControl().setLookAt(target, 30F, 30F);
        this.repathTicks = Math.max(this.repathTicks - 1, 0);
        if (this.repathTicks > 0
                || !mob.getSensing().hasLineOfSight(target)
                || !targetMoved(target) && mob.getRandom().nextFloat() >= 0.05F) {
            return;
        }

        this.lastX = target.getX();
        this.lastY = target.getY();
        this.lastZ = target.getZ();
        int delay = 4 + mob.getRandom().nextInt(7);
        double distance = mob.distanceToSqr(target);
        if (distance > 1024D) {
            delay += 10;
        } else if (distance > 256D) {
            delay += 5;
        }

        Vec3 destination = approachPos(target);
        if (!mob.getNavigation().moveTo(destination.x, destination.y, destination.z, speed))
            delay += 15;
        this.repathTicks = adjustedTickDelay(delay);
    }

    private boolean targetMoved(LivingEntity target) {
        return this.lastX == 0D && this.lastY == 0D && this.lastZ == 0D
                || target.distanceToSqr(this.lastX, this.lastY, this.lastZ) >= 1D;
    }

    private Vec3 approachPos(LivingEntity target) {
        Vec3 away = mob.position().subtract(target.position());
        double range = Math.min(away.length(), 20D) - 10D;
        away = away.normalize();
        return new Vec3(
                mob.getX() + away.x * range + mob.getRandom().nextGaussian() * 2D,
                mob.getY() + away.y - 5D + mob.getRandom().nextInt(11),
                mob.getZ() + away.z * range + mob.getRandom().nextGaussian() * 2D);
    }
}
