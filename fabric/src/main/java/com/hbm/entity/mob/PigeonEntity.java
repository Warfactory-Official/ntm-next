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

import com.hbm.entity.mob.ai.EatBreadGoal;
import com.hbm.entity.mob.ai.StartFlyingGoal;
import com.hbm.entity.mob.ai.StopFlyingGoal;
import com.hbm.entity.mob.ai.SwimmingConditionalGoal;
import com.hbm.entity.mob.ai.WanderConditionalGoal;
import com.hbm.util.FertilizerUtil;
import com.hbm.util.MobUtil;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PigeonEntity extends PathfinderMob {

    public static final String FAT_LOOT = "fat";

    private static final EntityDataAccessor<Byte> FLYING_STATE =
            SynchedEntityData.defineId(PigeonEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> FAT =
            SynchedEntityData.defineId(PigeonEntity.class, EntityDataSerializers.BYTE);

    public float fallTime;
    public float dest;
    public float prevDest;
    public float prevFallTime;
    public float offGroundTimer = 1.0F;
    private boolean scatterFeathers;

    public PigeonEntity(EntityType<? extends PigeonEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes();
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.fixed(0.5F, 1.0F);
    }

    @Override
    protected void registerGoals() {
        Predicate<PathfinderMob> grounded = p -> !((PigeonEntity) p).isFlying();
        goalSelector.addGoal(0, new StartFlyingGoal(this));
        goalSelector.addGoal(0, new StopFlyingGoal(this));
        goalSelector.addGoal(
                1, new SwimmingConditionalGoal(this, m -> grounded.test((PathfinderMob) m)));
        goalSelector.addGoal(2, new EatBreadGoal(this, 0.4D));
        goalSelector.addGoal(5, new WanderConditionalGoal(this, 0.2D, grounded));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLYING_STATE, (byte) 0);
        builder.define(FAT, (byte) 0);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        scatterFeathers = amount >= getMaxHealth() * 2;
        var hurt = super.hurtServer(level, source, amount);
        if (hurt && dead && scatterFeathers) discard();
        return hurt;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.CHICKEN_STEP.value(), 0.15F, 1.0F);
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled) {
        if (scatterFeathers) {
            for (int i = 0; i < 10; i++) {
                Vec3 vec =
                        new Vec3(
                                        random.nextGaussian(),
                                        random.nextGaussian(),
                                        random.nextGaussian())
                                .normalize();
                ItemEntity feather =
                        spawnAtLocation(
                                level,
                                new ItemStack(Items.FEATHER),
                                new Vec3(vec.x, getBbHeight() / 2D + vec.y, vec.z));
                feather.setDeltaMovement(vec.scale(0.5D));
                feather.setNoPickUpDelay();
            }
        } else if (isFat())
            dropFromLootTable(
                    level, source, playerKilled, MobUtil.lootVariant(getType(), FAT_LOOT));
        else super.dropFromLootTable(level, source, playerKilled);
    }

    public boolean isFlying() {
        return entityData.get(FLYING_STATE) != 0;
    }

    public void setFlying(boolean flying) {
        entityData.set(FLYING_STATE, (byte) (flying ? 1 : 0));
    }

    public boolean isFat() {
        return entityData.get(FAT) == 1;
    }

    public void setFat(boolean fat) {
        entityData.set(FAT, (byte) (fat ? 1 : 0));
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (isFlying()) {
            int ground =
                    level.getHeight(
                            Heightmap.Types.MOTION_BLOCKING,
                            (int) Math.floor(getX()),
                            (int) Math.floor(getZ()));
            boolean ceil = getY() - ground > 10;

            double motionY =
                    getRandom().nextGaussian() * 0.05 + (ceil ? 0 : 0.04) + (isInWater() ? 0.2 : 0);
            if (onGround()) motionY = Math.abs(motionY) + 0.1D;
            setDeltaMovement(getDeltaMovement().x, motionY, getDeltaMovement().z);

            zza = 1.5F;
            if (getRandom().nextInt(20) == 0)
                setYRot(getYRot() + (float) (getRandom().nextGaussian() * 30));

            if (isFat() && getRandom().nextInt(50) == 0) {
                level.sendParticles(
                        new BlockParticleOption(
                                ParticleTypes.BLOCK, Blocks.WOOL.white().defaultBlockState()),
                        getX(),
                        getY() + getBbHeight() / 2D,
                        getZ(),
                        3,
                        0.2D,
                        0.2D,
                        0.2D,
                        0D);

                BlockPos below = blockPosition();
                for (int i = 0; i < 25; i++) {
                    BlockPos target = below.below(i);
                    if (FertilizerUtil.fertilize(
                            level, target, null, ItemStack.EMPTY, true, BonemealSource.MOB)) {
                        FertilizerUtil.growthParticles(level, target);
                        break;
                    }
                }

                if (getRandom().nextInt(10) == 0) setFat(false);
            }

        } else if (!onGround() && getDeltaMovement().y < 0.0D) {
            setDeltaMovement(getDeltaMovement().multiply(1D, 0.8D, 1D));
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        prevFallTime = fallTime;
        prevDest = dest;
        dest += (onGround() ? -1 : 4) * 0.3F;

        if (dest < 0.0F) dest = 0.0F;
        if (dest > 1.0F) dest = 1.0F;

        if (!onGround() && offGroundTimer < 1.0F) offGroundTimer = 1.0F;
        offGroundTimer *= 0.9F;

        if (!onGround() && getDeltaMovement().y < 0.0D) {
            setDeltaMovement(getDeltaMovement().multiply(1D, 0.6D, 1D));
        }

        fallTime += offGroundTimer * 2.0F;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}
}
