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

package com.hbm.entity.mob.glyphid;

import com.hbm.blocks.ModBlocks;
import com.hbm.data.MobData;
import com.hbm.entity.logic.WaypointEntity;
import com.hbm.entity.mob.glyphid.GlyphidNuclearEntity;
import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm.handler.pollution.GlyphidSpawning;
import com.hbm.main.ResourceManager;
import com.hbm.util.MobUtil;
import com.hbm.util.RegistryUtil;
import com.hbm.world.feature.GlyphidHiveFeature;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

public class GlyphidScoutEntity extends GlyphidEntity {

    boolean hasTarget = false;
    int timer;
    int scoutingRange = 45;
    int minDistanceToHive = 8;
    boolean useLargeHive = false;

    public GlyphidScoutEntity(EntityType<? extends GlyphidScoutEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return GlyphidEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, GlyphidStats.getStats().getScout().health)
                .add(
                        Attributes.MOVEMENT_SPEED,
                        MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getScout().speed))
                .add(Attributes.ATTACK_DAMAGE, GlyphidStats.getStats().getScout().damage);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.fixed(1.25F, 0.75F);
    }

    @Override
    public Identifier getSkin() {
        return ResourceManager.glyphid_scout_tex;
    }

    @Override
    public double getGlyphidScale() {
        return 0.75D;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(GlyphidStats.getStats().getScout().health);
        getAttribute(Attributes.MOVEMENT_SPEED)
                .setBaseValue(MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getScout().speed));
        getAttribute(Attributes.ATTACK_DAMAGE)
                .setBaseValue(GlyphidStats.getStats().getScout().damage);
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsScout;
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return random.nextInt(100) <= Math.min(Math.pow(amount, 2), 100);
    }

    @Override
    public boolean isScoutType() {
        return true;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (super.doHurtTarget(level, target) && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 10 * 20, 3));
            return true;
        }
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (getTarget() != null && tickCount % 60 == 0) {
            setTarget(findTargetCandidate());
        }

        if (getCurrentTask() != TASK_BUILD_HIVE
                && getCurrentTask() != TASK_TERRAFORM
                && getWaypoint() == null) {

            if (MobData.rampantGlyphidGuidance() && GlyphidSpawning.guidance() != null) {

                if (!hasTarget) {
                    Vec3 dirVec =
                            playerBaseDirFinder(
                                    new Vec3(getX(), getY(), getZ()), getPlayerTargetDirection());

                    if (level() instanceof ServerLevel serverLevel && WaypointEntity.TYPE != null) {
                        WaypointEntity target =
                                new WaypointEntity(WaypointEntity.TYPE.get(), serverLevel);
                        target.snapTo(dirVec.x, dirVec.y, dirVec.z, 0F, 0F);
                        target.maxAge = 300;
                        target.radius = 6;
                        target.setWaypointType(TASK_BUILD_HIVE);
                        serverLevel.addFreshEntity(target);
                        hasTarget = true;

                        setCurrentTask(TASK_RETREAT_FOR_REINFORCEMENTS, target);
                    }
                }

                if (super.isAtDestination()) {
                    setCurrentTask(TASK_BUILD_HIVE, null);
                    hasTarget = false;
                }

            } else {
                setCurrentTask(TASK_BUILD_HIVE, null);
            }
        }

        if (getCurrentTask() == TASK_BUILD_HIVE || getCurrentTask() == TASK_TERRAFORM) {

            if (!level().isClientSide() && !hasTarget) {

                if (scoutingRange != 60 && hasNuclearGlyphidNearby()) {
                    setCurrentTask(TASK_TERRAFORM, null);
                }

                if (expandHive()) {
                    addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 180 * 20, 1));
                    hasTarget = true;
                }
            }

            if (getWaypoint() == null && hasTarget) {
                hasTarget = false;
            }

            if (getCurrentTask() == TASK_TERRAFORM
                    && super.isAtDestination()
                    && canBuildHiveHere(level)) {
                communicate(TASK_TERRAFORM, getWaypoint());
            }

            if (tickCount % 10 == 0 && isAtDestination()) {
                timer++;

                if (!level().isClientSide() && canBuildHiveHere(level)) {
                    if (timer == 1) {

                        if (level() instanceof ServerLevel serverLevel
                                && WaypointEntity.TYPE != null) {

                            WaypointEntity additional =
                                    new WaypointEntity(WaypointEntity.TYPE.get(), serverLevel);
                            additional.snapTo(getX(), getY(), getZ(), 0F, 0F);
                            additional.setWaypointType(TASK_IDLE);

                            WaypointEntity home =
                                    new WaypointEntity(WaypointEntity.TYPE.get(), serverLevel);
                            home.setWaypointType(TASK_RETREAT_FOR_REINFORCEMENTS);
                            home.setAdditionalWaypoint(additional);
                            home.snapTo(homeX, homeY, homeZ, 0F, 0F);
                            home.maxAge = 1200;
                            home.radius = 6;
                            serverLevel.addFreshEntity(home);

                            this.taskWaypoint = home;
                            addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40 * 20, 10));
                            communicate(TASK_RETREAT_FOR_REINFORCEMENTS, getWaypoint());
                        }

                    } else if (timer >= 5) {

                        if (level() instanceof ServerLevel serverLevel) {
                            serverLevel.explode(
                                    this,
                                    getX(),
                                    getY(),
                                    getZ(),
                                    5F,
                                    Level.ExplosionInteraction.NONE);
                            GlyphidHiveFeature.generateSmall(
                                    serverLevel,
                                    Mth.floor(getX()),
                                    Mth.floor(getY()),
                                    Mth.floor(getZ()),
                                    random,
                                    subtype() != TYPE_NORMAL,
                                    false);
                        }
                        discard();

                    } else {
                        communicate(TASK_FOLLOW, getWaypoint());
                    }
                }
            }
        }
    }

    public boolean canBuildHiveHere(ServerLevel level) {
        if (!((boolean) level.getGameRules().<java.lang.Boolean>get(GameRules.MOB_GRIEFING)))
            return false;
        int length = useLargeHive ? 16 : 8;

        for (int i = 0; i < 8; i++) {

            float angle = (float) Math.toRadians(360D / 16 * i);
            Vec3 rot = new Vec3(0, 0, length).yRot(angle);
            Vec3 from = new Vec3(getX(), getY() + 1, getZ());
            Vec3 to = new Vec3(getX() + rot.x, getY() + 1, getZ() + rot.z);

            ClipContext context =
                    new ClipContext(
                            from,
                            to,
                            ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.NONE,
                            CollisionContext.empty());
            BlockHitResult hit = level().clip(context);

            if (hit.getType() == HitResult.Type.BLOCK) {

                Block block = level().getBlockState(hit.getBlockPos()).getBlock();

                if (block == ModBlocks.GLYPHID_BASE.get()) {
                    setCurrentTask(TASK_IDLE, null);
                    hasTarget = false;
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isAtDestination() {
        return getCurrentTask() == TASK_BUILD_HIVE && super.isAtDestination();
    }

    public boolean hasNuclearGlyphidNearby() {
        int radius = 8;
        AABB bb = new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(radius);

        List<Entity> bugs = level().getEntities(this, bb);
        for (Entity e : bugs) {
            if (e instanceof GlyphidNuclearEntity) return true;
        }
        return false;
    }

    @Override
    public boolean expandHive() {

        int nestX =
                random.nextInt((homeX + scoutingRange) - (homeX - scoutingRange))
                        + (homeX - scoutingRange);
        int nestZ =
                random.nextInt((homeZ + scoutingRange) - (homeZ - scoutingRange))
                        + (homeZ - scoutingRange);
        int nestY = level().getHeight(Heightmap.Types.MOTION_BLOCKING, nestX, nestZ);
        BlockState state = level().getBlockState(new BlockPos(nestX, nestY - 1, nestZ));
        Block b = state.getBlock();

        boolean distanceCheck =
                new Vec3(nestX - homeX, nestY - homeY, nestZ - homeZ).length() > minDistanceToHive;

        if (distanceCheck
                && !state.isAir()
                && state.isSolidRender()
                && b != ModBlocks.GLYPHID_BASE.get()) {

            if (isBasalt(b)) {
                useLargeHive = true;
                addEffect(new MobEffectInstance(MobEffects.SPEED, 60 * 20, 3));
            }

            if (!level().isClientSide()
                    && level() instanceof ServerLevel serverLevel
                    && WaypointEntity.TYPE != null) {

                WaypointEntity nest = new WaypointEntity(WaypointEntity.TYPE.get(), serverLevel);
                nest.setWaypointType(getCurrentTask());
                nest.radius = 5;

                if (useLargeHive) nest.setHighPriority();

                nest.snapTo(nestX, nestY, nestZ, 0F, 0F);
                serverLevel.addFreshEntity(nest);

                this.taskWaypoint = nest;

                setCurrentTask(getCurrentTask(), this.taskWaypoint);
                communicate(TASK_BUILD_HIVE, this.taskWaypoint);
            }

            return true;
        }

        return false;
    }

    private static boolean isBasalt(Block block) {
        return BuiltInRegistries.BLOCK
                .getOptional(RegistryUtil.id("basalt"))
                .map(basalt -> basalt == block)
                .orElse(false);
    }

    @Override
    public void carryOutTask() {
        if (!level().isClientSide() && getWaypoint() == null) {
            switch (getCurrentTask()) {
                case TASK_INITIATE_RETREAT -> {
                    removeEffect(MobEffects.SLOWNESS);
                    addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 20, 4));

                    if (level() instanceof ServerLevel serverLevel && WaypointEntity.TYPE != null) {

                        WaypointEntity additional =
                                new WaypointEntity(WaypointEntity.TYPE.get(), serverLevel);
                        additional.snapTo(getX(), getY(), getZ(), 0F, 0F);
                        additional.setWaypointType(TASK_IDLE);

                        WaypointEntity home =
                                new WaypointEntity(WaypointEntity.TYPE.get(), serverLevel);
                        home.setWaypointType(TASK_BUILD_HIVE);
                        home.setAdditionalWaypoint(additional);
                        home.setHighPriority();
                        home.radius = 6;
                        home.snapTo(homeX, homeY, homeZ, 0F, 0F);
                        serverLevel.addFreshEntity(home);

                        communicate(TASK_FOLLOW, home);
                    }
                }

                case TASK_TERRAFORM -> {
                    scoutingRange = 60;
                    minDistanceToHive = 20;
                }

                default -> {}
            }
        }
        super.carryOutTask();
    }

    @Override
    public boolean useExtendedTargeting() {
        return false;
    }

    @Override
    public @Nullable Player findTargetCandidate() {
        if (hasEffect(MobEffects.BLINDNESS)) return null;
        return level().getNearestPlayer(getX(), getY(), getZ(), 10D, true);
    }

    public static Vec3 playerBaseDirFinder(Vec3 currentLocation, Vec3 target) {
        Vec3 dirVec = target.subtract(currentLocation).normalize();
        return new Vec3(
                currentLocation.x + dirVec.x * 10,
                currentLocation.y + dirVec.y * 10,
                currentLocation.z + dirVec.z * 10);
    }

    protected @Nullable Vec3 getPlayerTargetDirection() {
        Player player = level().getNearestPlayer(getX(), getY(), getZ(), 300D, false);
        if (player != null) return player.position();
        return GlyphidSpawning.guidance();
    }

    public boolean isValidLightLevel() {
        BlockPos pos =
                new BlockPos(
                        Mth.floor(getX()), Mth.floor(getBoundingBox().minY), Mth.floor(getZ()));
        int skyDarken = level().isThundering() ? 10 : level().getSkyDarken();
        return level().getMaxLocalRawBrightness(pos, skyDarken) <= 7;
    }
}
