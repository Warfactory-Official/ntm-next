/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

package com.hbm.entity.projectile;

import com.hbm.blocks.bomb.ProjectileHitHandler;
import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.CreeperNuclearEntity;
import com.hbm.entity.projectile.ProjectileEntity;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.ModItems;
import com.hbm.lib.ModDamageTypes;
import com.hbm.lib.ModifiedDamageSource;
import com.hbm.sound.WorldSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class BulletEntity extends ProjectileEntity {
    public enum Mode {
        BULLET,
        TAU,
        CHOPPER
    }

    private static final Mode[] MODES = Mode.values();

    private static final EntityDataAccessor<Integer> MODE =
            SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.INT);
    public double gravity;
    public double damage;
    public boolean antidote;
    public boolean fire;
    private boolean instakill;
    private boolean radioactive;
    private int knockbackStrength;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
    }

    public BulletEntity(
            EntityType<? extends BulletEntity> type,
            Level level,
            LivingEntity shooter,
            LivingEntity target,
            float velocity,
            float inaccuracy,
            Mode mode) {
        this(type, level);
        setOwner(shooter);
        setMode(mode);
        var delta =
                target.position()
                        .add(0, target.getBbHeight() / 3.0, 0)
                        .subtract(shooter.getEyePosition().subtract(0, 0.1, 0));
        var horizontal = delta.multiply(1, 0, 1).normalize();
        setPos(shooter.getEyePosition().subtract(0, 0.1, 0).add(horizontal));
        shoot(delta.x, delta.y, delta.z, velocity, inaccuracy);
    }

    public BulletEntity(
            EntityType<? extends BulletEntity> type,
            Level level,
            LivingEntity shooter,
            float velocity,
            Mode mode) {
        this(type, level);
        initThrower(shooter, velocity * 1.5F, 1);
        setMode(mode);
    }

    public BulletEntity(
            EntityType<? extends BulletEntity> type,
            Level level,
            LivingEntity shooter,
            float velocity,
            boolean instakill,
            boolean radioactive) {
        this(type, level, shooter, velocity, Mode.BULLET);
        this.instakill = instakill;
        this.radioactive = radioactive;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MODE, Mode.BULLET.ordinal());
    }

    public Mode getMode() {
        return MODES[entityData.get(MODE)];
    }

    public void setMode(Mode mode) {
        entityData.set(MODE, mode.ordinal());
    }

    public boolean getIsCritical() {
        return getMode() == Mode.TAU;
    }

    public boolean getIsTau() {
        return getMode() == Mode.TAU;
    }

    public boolean getIsChopper() {
        return getMode() == Mode.CHOPPER;
    }

    @Override
    public boolean isSpectral() {
        return getMode() == Mode.TAU;
    }

    @Override
    public boolean doesPenetrate() {
        return getMode() == Mode.TAU;
    }

    @Override
    public double getGravityVelocity() {
        return gravity;
    }

    @Override
    public void tick() {
        var from = position();
        super.tick();
        if (tickCount > 250) discard();
        if (level().isClientSide() && getIsTau()) {
            var step = position().subtract(from);
            for (int i = 0; i < 8; ++i) {
                var particle = from.add(step.scale(i / 8.0));
                level().addParticle(
                                DustParticleOptions.REDSTONE,
                                particle.x,
                                particle.y,
                                particle.z,
                                0,
                                0,
                                0);
            }
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target)
                && (!(target instanceof Player player)
                        || !player.getAbilities().invulnerable
                                && (!(getOwner() instanceof Player shooter)
                                        || shooter.canHarmPlayer(player)));
    }

    @Override
    protected void onImpact(HitResult result) {
        if (!(level() instanceof ServerLevel server)) return;
        if (result instanceof EntityHitResult entityHit) {
            hitEntity(server, entityHit.getEntity());
        } else if (result instanceof BlockHitResult blockHit) {
            var pos = blockHit.getBlockPos();
            var state = server.getBlockState(pos);
            if (state.getBlock() instanceof ProjectileHitHandler detonatable)
                detonatable.onShot(server, pos);
            if (state.is(Blocks.GLASS)
                    || state.getBlock() instanceof StainedGlassBlock
                    || state.is(Blocks.GLASS_PANE)
                    || state.getBlock() instanceof StainedGlassPaneBlock) {
                server.removeBlock(pos, false);
                WorldSounds.play(
                        server,
                        null,
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        SoundEvents.GLASS_BREAK,
                        SoundSource.BLOCKS,
                        1,
                        1);
            }
            discard();
        }
    }

    private void hitEntity(ServerLevel server, Entity target) {
        if (target instanceof ItemFrame frame && frame.getItem().is(ModItems.FLAME_PONY.get())) {
            discard();
            return;
        }
        var type =
                switch (getMode()) {
                    case BULLET -> ModDamageTypes.REVOLVER_BULLET;
                    case TAU -> ModDamageTypes.TAU;
                    case CHOPPER -> ModDamageTypes.EMPLACER;
                };
        float amount = (float) damage;
        if (instakill
                && target instanceof LivingEntity
                && !(target instanceof Player)
                && !(target instanceof EnderMan)) {
            type = ModDamageTypes.LETHAL_PROJECTILE;
            amount = Float.MAX_VALUE;
        }
        DamageSource source = server.damageSources().source(type, this, getOwner());
        if (fire || isOnFire() && !(target instanceof EnderMan)) target.igniteForSeconds(5);
        if (target.hurtServer(server, ModifiedDamageSource.bypassingCooldown(source), amount)) {
            if (target instanceof LivingEntity living) {
                if (radioactive && living.isAlive()) irradiate(server, living);
                if (antidote) living.removeAllEffects();
                if (knockbackStrength > 0) {
                    var horizontal =
                            getDeltaMovement()
                                    .multiply(1, 0, 1)
                                    .normalize()
                                    .scale(knockbackStrength * 0.6);
                    living.push(horizontal.x, 0.1, horizontal.z);
                }
                if (getOwner() instanceof LivingEntity) {
                    EnchantmentHelper.doPostAttackEffectsWithItemSource(
                            server, living, source, null);
                }
                if (getOwner() instanceof ServerPlayer shooter
                        && target != shooter
                        && target instanceof Player) {
                    shooter.connection.send(
                            new ClientboundGameEventPacket(
                                    ClientboundGameEventPacket.PLAY_ARROW_HIT_SOUND, 0));
                }
            }
        }
        if (getMode() != Mode.TAU) discard();
    }

    private void irradiate(ServerLevel server, LivingEntity target) {
        if (target instanceof Player && ArmorUtil.checkForHazmat(target)) return;
        LivingEntity replacement;
        if (target instanceof Creeper) {
            replacement = new CreeperNuclearEntity(ModEntities.CREEPER_NUCLEAR.get(), server);
        } else if (target instanceof Villager) {
            replacement = new Zombie(server);
        } else {
            if (!(target instanceof Zombie)
                    && !(target instanceof MushroomCow)
                    && !(target instanceof CreeperNuclearEntity)) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 2400, 2));
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 20, 4));
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 1200, 1));
            }
            return;
        }
        replacement.snapTo(
                target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        if (server.addFreshEntity(replacement)) target.discard();
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public double getDamage() {
        return damage;
    }

    public void setKnockbackStrength(int strength) {
        knockbackStrength = strength;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(0.5F, 0.5F);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = getBoundingBox().getSize() * 10 * 64;
        return distance < range * range;
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return getMode() == Mode.BULLET ? super.getLightLevelDependentMagicValue() : 1;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Mode", getMode().name());
        output.putDouble("Damage", damage);
        output.putDouble("Gravity", gravity);
        output.putBoolean("InstantKill", instakill);
        output.putBoolean("Radioactive", radioactive);
        output.putBoolean("Antidote", antidote);
        output.putBoolean("Fire", fire);
        output.putInt("Knockback", knockbackStrength);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setMode(Mode.valueOf(input.getStringOr("Mode", getMode().name())));
        damage = input.getDoubleOr("Damage", 0);
        gravity = input.getDoubleOr("Gravity", 0);
        instakill = input.getBooleanOr("InstantKill", false);
        radioactive = input.getBooleanOr("Radioactive", false);
        antidote = input.getBooleanOr("Antidote", false);
        fire = input.getBooleanOr("Fire", false);
        knockbackStrength = input.getIntOr("Knockback", 0);
    }
}
