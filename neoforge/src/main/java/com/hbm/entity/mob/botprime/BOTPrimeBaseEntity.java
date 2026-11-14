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

package com.hbm.entity.mob.botprime;

import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.items.weapon.sedna.factory.XFactoryNPC;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class BOTPrimeBaseEntity extends WormBaseNTEntity {

    public int attackCounter;

    protected BOTPrimeBaseEntity(EntityType<? extends BOTPrimeBaseEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.dragInAir = 0.995F;
        this.dragInGround = 0.98F;
        this.knockbackDivider = 1.0D;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WormBaseNTEntity.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 15000.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    public boolean canSeeThroughNonSolids(Entity target) {
        Vec3 from = new Vec3(getX(), getY() + getEyeHeight(), getZ());
        Vec3 to = new Vec3(target.getX(), target.getY() + target.getEyeHeight(), target.getZ());
        return level().clip(
                                new ClipContext(
                                        from,
                                        to,
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        this))
                        .getType()
                == HitResult.Type.MISS;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return isDecaying() ? null : SoundEvents.BLAZE_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return isDecaying() ? null : ModSounds.BOMB_DET.get();
    }

    protected void laserAttack(Entity target, boolean head) {

        if (!(target instanceof LivingEntity)) return;

        if (head) {

            for (int i = 0; i < 5; i++) {
                BulletBaseMK4Entity bullet =
                        XFactoryNPC.aimed(
                                this,
                                XFactoryNPC.worm_laser,
                                XFactoryNPC.WORM_LASER_DAMAGE,
                                target,
                                i * 0.05F);
                if (bullet != null) level().addFreshEntity(bullet);
            }

            playLaser(0.75F);

        } else {
            BulletBaseMK4Entity bullet =
                    XFactoryNPC.aimed(
                            this,
                            XFactoryNPC.worm_bolt,
                            XFactoryNPC.WORM_BOLT_DAMAGE,
                            target,
                            0.125F);
            if (bullet != null) level().addFreshEntity(bullet);
            playLaser(1.0F);
        }
    }

    private void playLaser(float pitch) {
        WorldSounds.play(
                level(),
                null,
                getX(),
                getY(),
                getZ(),
                ModSounds.BALLS_LASER.get(),
                SoundSource.HOSTILE,
                5F,
                pitch);
    }
}
