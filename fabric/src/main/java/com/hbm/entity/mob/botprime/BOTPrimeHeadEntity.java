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

import com.hbm.advancement.BossKilledTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.ai.NearestAttackableTargetNTGoal;
import com.hbm.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BOTPrimeHeadEntity extends BOTPrimeBaseEntity {

    private static final int BODY_SEGMENTS = 74;
    private static final double ATTACK_RANGE_SQ = 150D * 150D;
    private static final double MAX_NUDGE_SPEED_SQ = 1D;
    private static final double DRIFT_DOWN = 0.006D;
    private static final double SURFACE_Y = 60D;
    private static final double DIVE_Y = 10D;
    private static final double DIVE_DONE_Y = 15D;
    private static final int LASER_CHARGE = 30;

    private final ServerBossEvent bossEvent =
            new ServerBossEvent(
                    getUUID(),
                    getDisplayName(),
                    BossEvent.BossBarColor.PURPLE,
                    BossEvent.BossBarOverlay.PROGRESS);

    private BlockPos spawnPoint = BlockPos.ZERO;
    private int courseChangeCooldown;
    private int aggroCooldown;
    private boolean wasNearGround;

    public BOTPrimeHeadEntity(EntityType<? extends BOTPrimeHeadEntity> type, Level level) {
        super(type, level);
        this.xpReward = 1000;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BOTPrimeBaseEntity.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.15D)
                .add(Attributes.FOLLOW_RANGE, 128.0D);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(
                2, new NearestAttackableTargetNTGoal<>(this, Player.class, 0, false, false, null));
    }

    @Override
    public boolean getIsHead() {
        return true;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {
        setHeadID(getId());
        var origin = blockPosition();
        for (int part = 0; part < BODY_SEGMENTS; part++) {
            var segment = ModEntities.BALLS_O_TRON_SEG.get().create(level.getLevel(), reason);
            segment.setPartNumber(part);
            segment.setHeadID(getId());
            segment.setPos(origin.getX(), origin.getY(), origin.getZ());
            level.addFreshEntity(segment);
        }
        setPos(origin.getX(), origin.getY(), origin.getZ());
        spawnPoint = origin;
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        updateActionState(level);
        super.customServerAiStep(level);

        boolean underground = isBuried();
        nudgeTowardWaypoint(underground);
        aggroCooldown--;
        pickQuarry();
        chaseQuarry(underground);

        regenerate();
        chargeLaser();
    }

    private void nudgeTowardWaypoint(boolean underground) {
        double dx = waypointX - getX();
        double dy = waypointY - getY();
        double dz = waypointZ - getZ();

        if (courseChangeCooldown-- <= 0) {
            courseChangeCooldown += getRandom().nextInt(5) + 2;
            var motion = getDeltaMovement();
            if (motion.lengthSqr() < MAX_NUDGE_SPEED_SQ) {
                double reach = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (!underground) reach *= 8D;
                double speed = getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
                setDeltaMovement(
                        motion.add(dx / reach * speed, dy / reach * speed, dz / reach * speed));
            }
        }

        if (!underground) setDeltaMovement(getDeltaMovement().subtract(0D, DRIFT_DOWN, 0D));
    }

    private void pickQuarry() {
        var target = getTarget();
        if (target == null) {
            if (targetedEntity == null) wander();
            return;
        }
        if (aggroCooldown <= 0) {
            targetedEntity = target;
            aggroCooldown = 20;
        }
    }

    private void wander() {
        var random = getRandom();
        waypointX = spawnPoint.getX() - 50 + random.nextInt(100);
        waypointY = spawnPoint.getY() - 30 + random.nextInt(60);
        waypointZ = spawnPoint.getZ() - 50 + random.nextInt(100);
    }

    private void chaseQuarry(boolean underground) {
        var quarry = targetedEntity;
        if (quarry == null || quarry.distanceToSqr(this) >= ATTACK_RANGE_SQ) return;

        waypointX = quarry.getX();
        waypointZ = quarry.getZ();
        if (!wasNearGround) {
            waypointY = DIVE_Y;
            if (getY() < DIVE_DONE_Y) wasNearGround = true;
            return;
        }
        waypointY = quarry.getY();
        if (getRandom().nextInt(80) == 0 && getY() > SURFACE_Y && !underground)
            wasNearGround = false;
    }

    private void regenerate() {
        if (tickCount % 6 != 0 || getHealth() >= getMaxHealth()) return;
        if (targetedEntity != null) {
            heal(1F);
        } else if (getLastHurtByPlayer() == null) {
            heal(4F);
        }
    }

    private void chargeLaser() {
        var quarry = targetedEntity;
        if (quarry == null
                || quarry.distanceToSqr(this) >= ATTACK_RANGE_SQ
                || !canSeeThroughNonSolids(quarry)) {
            attackCounter = 0;
            return;
        }
        if (++attackCounter == LASER_CHARGE) {
            laserAttack(quarry, true);
            attackCounter = 0;
        }
    }

    @Override
    public void tick() {
        super.tick();
        var motion = getDeltaMovement();
        faceAlong(motion.x, motion.y, motion.z);
        if (!level().isClientSide()) bossEvent.setProgress(getHealth() / getMaxHealth());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    protected void handleKillingBlow() {
        super.handleKillingBlow();

        HbmCriteria.bossKilled(this, BossKilledTrigger.Kind.WORM, 200D);

        if (level() instanceof ServerLevel server) {
            for (Player player :
                    server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(200D))) {
                player.getInventory()
                        .placeItemBackInInventory(
                                new ItemStack(ModItems.COIN_WORM.get()), Prediction.SERVER_ONLY);
            }
        }
    }

    @Override
    public float getAttackStrength(Entity target) {
        return 1000F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("spawnX", spawnPoint.getX());
        output.putInt("spawnY", spawnPoint.getY());
        output.putInt("spawnZ", spawnPoint.getZ());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        spawnPoint =
                new BlockPos(
                        input.getIntOr("spawnX", 0),
                        input.getIntOr("spawnY", 0),
                        input.getIntOr("spawnZ", 0));
    }
}
