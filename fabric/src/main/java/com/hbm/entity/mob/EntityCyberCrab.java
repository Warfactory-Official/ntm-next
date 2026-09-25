// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityBullet;
import com.hbm.interfaces.IRadiationImmune;
import com.hbm.lib.ModDamageTypes;
import com.hbm.sound.ModSounds;
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

public class EntityCyberCrab extends Monster implements RangedAttackMob, IRadiationImmune {

    public EntityCyberCrab(EntityType<? extends EntityCyberCrab> type, Level level) {
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
        if (!(this instanceof EntityTaintCrab)) goalSelector.addGoal(0, new PanicGoal(this, 0.75D));
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
                                !(target instanceof EntityCyberCrab || target instanceof Creeper)));
    }

    protected Goal rangedGoal() {
        return new RangedAttackGoal(this, 0.5D, 60, 80, 15.0F);
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

        if (getHealth() <= 0) {
            discard();

            server.explode(
                    this,
                    getX(),
                    getY(),
                    getZ(),
                    this instanceof EntityTaintCrab ? 3F : 0.1F,
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
        EntityBullet bullet =
                new EntityBullet(ModEntities.BULLET.get(), level(), this, target, 1.6F, 2F);
        bullet.setIsCritical(true);
        bullet.setTau(true);
        bullet.damage = 3;
        level().addFreshEntity(bullet);
        playSound(ModSounds.SAW_SHOOT.get(), 1.0F, 2.0F);
    }
}
