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

package com.hbm.entity.mob;

import com.hbm.api.environment.SuffocationImmune;
import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.BulletEntity;
import com.hbm.lib.ModDamageTypes;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;

public class CyberCrabEntity extends Monster implements RangedAttackMob, SuffocationImmune {

    public CyberCrabEntity(EntityType<? extends CyberCrabEntity> type, Level level) {
        super(type, level);
        setPathfindingMalus(PathType.WATER, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.75D);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.fixed(0.75F, 0.35F);
    }

    @Override
    protected void registerGoals() {
        if (canPanic()) goalSelector.addGoal(0, new PanicGoal(this, 0.75D));
        goalSelector.addGoal(1, new RandomStrollGoal(this, 0.5D));
        goalSelector.addGoal(4, rangedGoal());
        targetSelector.addGoal(
                1, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, null));
        targetSelector.addGoal(
                2,
                new NearestAttackableTargetGoal<>(
                        this,
                        Mob.class,
                        0,
                        true,
                        true,
                        (target, level) ->
                                !(target instanceof CyberCrabEntity || target instanceof Creeper)));
    }

    protected Goal rangedGoal() {
        return new RangedAttackGoal(this, 0.5D, 60, 80, 15.0F);
    }

    protected boolean canPanic() {
        return true;
    }

    protected float deathExplosionStrength() {
        return 0.1F;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(ModDamageTypes.TAU)) return false;
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void tick() {
        super.tick();

        if (!(level() instanceof ServerLevel server)) return;

        if (isInWater() || isInWaterOrRain() || isOnFire()) {
            hurtServer(server, level().damageSources().generic(), 10F);
        }

        if (dead) discard();
    }

    @Override
    protected void handleKillingBlow() {
        super.handleKillingBlow();
        if (level() instanceof ServerLevel server) {
            server.explode(
                    this,
                    getX(),
                    getY(),
                    getZ(),
                    deathExplosionStrength(),
                    Level.ExplosionInteraction.NONE);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_CYBERCRAB.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_CYBERCRAB.get();
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        return true;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        BulletEntity bullet =
                new BulletEntity(
                        ModEntities.BULLET.get(),
                        level(),
                        this,
                        target,
                        1.6F,
                        2F,
                        BulletEntity.Mode.TAU);
        bullet.damage = 3;
        level().addFreshEntity(bullet);
        WorldSounds.play(this, ModSounds.SAW_SHOOT.get(), 1.0F, 2.0F);
    }
}
