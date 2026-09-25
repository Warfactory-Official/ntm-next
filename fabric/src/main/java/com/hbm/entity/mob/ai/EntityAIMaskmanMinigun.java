// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.ai;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.factory.XFactory762mm;
import com.hbm.sound.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

public class EntityAIMaskmanMinigun extends Goal {

    private final PathfinderMob owner;
    private final int delay;
    private int timer;

    public EntityAIMaskmanMinigun(PathfinderMob owner, int delay) {
        this.owner = owner;
        this.delay = delay;
        this.timer = delay;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.owner.getTarget();
        if (target == null || !target.isAlive()) return false;
        double dist = Math.sqrt(this.owner.distanceToSqr(target));
        return dist > 5D && dist < 10D;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() || !this.owner.getNavigation().isDone();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {

        this.timer--;

        LivingEntity target = this.owner.getTarget();
        if (target != null) this.owner.getLookControl().setLookAt(target, 15F, 15F);

        if (this.timer > 0) return;
        this.timer = this.delay;

        EntityBulletBaseMK4 bullet =
                new EntityBulletBaseMK4(
                        this.owner, XFactory762mm.r762_fmj, 5F, 0.075F, -1.5D, -1.5D, 0D);

        this.owner.level().addFreshEntity(bullet);
        this.owner
                .level()
                .playSound(
                        null,
                        this.owner.getX(),
                        this.owner.getY(),
                        this.owner.getZ(),
                        ModSounds.GUN_MINIGUN_FIRE.get(),
                        SoundSource.HOSTILE,
                        1F,
                        1F);
    }
}
