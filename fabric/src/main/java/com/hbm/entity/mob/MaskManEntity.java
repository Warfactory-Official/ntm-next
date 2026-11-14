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

package com.hbm.entity.mob;

import com.hbm.advancement.BossKilledTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.api.environment.SuffocationImmune;
import com.hbm.entity.mob.ai.MaskmanCasualApproachGoal;
import com.hbm.entity.mob.ai.MaskmanLasergunGoal;
import com.hbm.entity.mob.ai.MaskmanMinigunGoal;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class MaskManEntity extends EggVulnerableEntity implements SuffocationImmune {

    private final ServerBossEvent bossEvent =
            new ServerBossEvent(
                    getUUID(),
                    getDisplayName(),
                    BossEvent.BossBarColor.RED,
                    BossEvent.BossBarOverlay.PROGRESS);

    private float lastHealth;

    public MaskManEntity(EntityType<? extends MaskManEntity> type, Level level) {
        super(type, level);
        this.xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 100.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new MaskmanCasualApproachGoal.Navigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MaskmanCasualApproachGoal(this, 1.0D, false));
        this.goalSelector.addGoal(2, new MaskmanMinigunGoal(this, 3));
        this.goalSelector.addGoal(3, new MaskmanLasergunGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {

        if (source.is(DamageTypeTags.IS_FIRE)) amount = 0F;
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) amount = 0F;
        if (source.is(DamageTypeTags.IS_PROJECTILE)) amount *= 0.5F;
        if (source.is(DamageTypeTags.IS_EXPLOSION)) amount *= 0.5F;

        if (amount > 50F) amount = 50F + (amount - 50F) * 0.5F;

        return super.hurtServer(level, source, amount);
    }

    @Override
    public void tick() {
        super.tick();

        if (level() instanceof ServerLevel server) {

            if (this.lastHealth >= getMaxHealth() / 2F
                    && getHealth() < getMaxHealth() / 2F
                    && isAlive()) {
                this.lastHealth = getHealth();
                server.explode(
                        this, getX(), getY() + 4, getZ(), 2.5F, Level.ExplosionInteraction.BLOCK);
            }

            this.lastHealth = getHealth();

            bossEvent.setProgress(getHealth() / getMaxHealth());
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        LivingEntity target = getTarget();
        if (target == null) return;

        getLookControl().setLookAt(target, 30F, 30F);
        setYRot(getYHeadRot());
        this.yBodyRot = getYRot();
    }

    @Override
    protected void handleKillingBlow() {
        super.handleKillingBlow();
        HbmCriteria.bossKilled(this, BossKilledTrigger.Kind.MASKMAN, 100D);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }
}
