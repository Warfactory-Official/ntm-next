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

package com.hbm.entity.mob.botprime;

import com.hbm.entity.mob.ai.NearestAttackableTargetNTGoal;
import com.hbm.entity.mob.botprime.BOTPrimeBaseEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BOTPrimeBodyEntity extends BOTPrimeBaseEntity {

    private static final int BOLT_CHARGE = 10;
    private static final int BOLT_RECOVERY = -20;

    public BOTPrimeBodyEntity(EntityType<? extends BOTPrimeBodyEntity> type, Level level) {
        super(type, level);
        this.rangeForParts = 70D;
        this.segmentDistance = 3.5D;
        this.maxBodySpeed = 1.4D;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BOTPrimeBaseEntity.createAttributes().add(Attributes.FOLLOW_RANGE, 128.0D);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(
                1,
                new NearestAttackableTargetNTGoal<Player>(
                        this,
                        Player.class,
                        0,
                        false,
                        false,
                        (entity, _) -> !isKin(entity)));
    }

    @Override
    public float getAttackStrength(Entity target) {
        if (target instanceof LivingEntity living) return living.getHealth() * 0.75F;
        return 100F;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        updateActionState(level);
        updateMovement(level);

        if (this.didCheck) {
            if (!isLiving(this.targetedEntity)) decay(level, 1999F);
            if (!isLiving(this.followed) && this.random.nextInt(60) == 0) {
                level.explode(this, getX(), getY(), getZ(), 2F, Level.ExplosionInteraction.NONE);
            }
        }

        var prey = getTarget();
        if (isLiving(this.followed) && prey != null && canSeeThroughNonSolids(prey)) {
            if (++this.attackCounter == BOLT_CHARGE) {
                laserAttack(prey, false);
                this.attackCounter = BOLT_RECOVERY;
            }
        } else if (this.attackCounter > 0) {
            this.attackCounter--;
        }

        faceLeader();
    }

    private static boolean isLiving(@Nullable Entity entity) {
        return entity != null && entity.isAlive();
    }

    @Override
    public void tick() {
        super.tick();
        faceLeader();
    }

    private void faceLeader() {
        var leader = this.targetedEntity;
        if (leader != null)
            faceAlong(leader.getX() - getX(), leader.getY() - getY(), leader.getZ() - getZ());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("partID", getPartNumber());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setPartNumber(input.getIntOr("partID", 0));
    }
}
