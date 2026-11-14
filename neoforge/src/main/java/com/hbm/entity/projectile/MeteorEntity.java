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

import com.hbm.api.entity.RadarProvider;
import com.hbm.api.entity.RadarScanParams;
import com.hbm.api.entity.RadarTargetType;
import com.hbm.blocks.ModBlocks;
import com.hbm.client.ClientEffects;
import com.hbm.client.ClientPlayerAccess;
import com.hbm.data.WorldData;
import com.hbm.entity.FarVisible;
import com.hbm.entity.ModEntities;
import com.hbm.explosion.BlastDebris;
import com.hbm.explosion.BlastSmoke;
import com.hbm.handler.resistance.DamageCategory;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import com.hbm.sound.WorldSounds;
import com.hbm.util.DropHeight;
import com.hbm.world.feature.Meteorite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class MeteorEntity extends Entity implements RadarProvider, FarVisible {

    public boolean safe = false;
    private SoundHandle audioFly;

    public MeteorEntity(EntityType<? extends MeteorEntity> type, Level level) {
        super(type, level);
    }

    public MeteorEntity(Level level) {
        this(ModEntities.METEOR.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        if (!level().isClientSide() && !WorldData.ENABLE_METEOR_STRIKES.get()) {
            discard();
            return;
        }

        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();

        Vec3 motion = getDeltaMovement();
        setDeltaMovement(motion.x, Math.max(motion.y - 0.03D, -2.5D), motion.z);
        move(MoverType.SELF, getDeltaMovement());
        if (!level().isClientSide() && !onGround()) WorldSounds.entityLoop(this, tickCount == 1);

        if (level() instanceof ServerLevel server
                && getY() < DropHeight.clearOfTerrain(server, 260, getX(), getZ())) {
            clearMeteorPath(server, (int) getX(), (int) getY(), (int) getZ());

            if (onGround()) {
                server.explode(
                        this,
                        null,
                        null,
                        getX(),
                        getY(),
                        getZ(),
                        5F + random.nextFloat(),
                        false,
                        safe ? Level.ExplosionInteraction.NONE : Level.ExplosionInteraction.BLOCK);

                if (WorldData.ENABLE_METEOR_TAILS.get()) {
                    BlastDebris.rubble(server, getX(), getY(), getZ(), 15);

                    BlastSmoke.cloud(server, getX(), getY() + 5, getZ(), 75);
                    BlastSmoke.cloud(server, getX() + 5, getY(), getZ(), 75);
                    BlastSmoke.cloud(server, getX() - 5, getY(), getZ(), 75);
                    BlastSmoke.cloud(server, getX(), getY(), getZ() + 5, 75);
                    BlastSmoke.cloud(server, getX(), getY(), getZ() - 5, 75);
                }

                double motionZ = getDeltaMovement().z;
                int spawnPosX = (int) (Math.round(getX() - 0.5D) + (safe ? 0 : (motionZ * 4)));
                int spawnPosY = (int) Math.round(getY() - (safe ? 0 : 4));
                int spawnPosZ = (int) (Math.round(getZ() - 0.5D) + (safe ? 0 : (motionZ * 4)));

                Meteorite.INSTANCE.generate(
                        server,
                        random,
                        new BlockPos(spawnPosX, spawnPosY, spawnPosZ),
                        safe,
                        true,
                        true);
                clearMeteorPath(server, spawnPosX, spawnPosY, spawnPosZ);

                WorldSounds.play(
                        level(),
                        null,
                        getX(),
                        getY(),
                        getZ(),
                        ModSounds.OLD_EXPLOSION.get(),
                        SoundSource.HOSTILE,
                        10000.0F,
                        0.5F + random.nextFloat() * 0.1F);

                discard();
            }
        }

        if (level().isClientSide()) {
            tickClientAudio();

            if (WorldData.ENABLE_METEOR_TAILS.get()) {
                Vec3 step = getDeltaMovement();
                for (int i = 0; i < 10; i++) {
                    ClientEffects.spawnRocketFlame(
                            level(),
                            getX() - step.x + random.nextGaussian(),
                            getY() - step.y + random.nextGaussian(),
                            getZ() - step.z + random.nextGaussian(),
                            1F,
                            0D,
                            0D,
                            0D,
                            300 + random.nextInt(50));
                }
            }
        }
    }

    private void tickClientAudio() {
        if (isRemoved()) {
            if (audioFly != null) audioFly.stop();
            return;
        }

        if (audioFly == null) {
            audioFly =
                    AudioSystem.getLoopedSound(
                            ModSounds.METEORITE_FALLING_LOOP.get(),
                            SoundSource.BLOCKS,
                            0F,
                            0F,
                            0F,
                            1F,
                            200F,
                            0.9F + random.nextFloat() * 0.2F,
                            10);
        }
        if (audioFly == null) return;

        if (audioFly.isPlaying()) {
            audioFly.refresh();
            audioFly.setVolume(1F);
            audioFly.moveTo((float) getX(), (float) (getY() + getBbHeight() / 2F), (float) getZ());
        } else {
            Player player = ClientPlayerAccess.player();
            if (player != null
                    && player.getEyePosition().distanceToSqr(getX(), getY(), getZ()) < 210D * 210D)
                audioFly.start();
        }
    }

    private void clearMeteorPath(ServerLevel level, int x, int y, int z) {
        int radius = 5;
        int rSq = radius * radius;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= rSq) {
                        damageOrDestroyBlock(level, pos.set(x + dx, y + dy, z + dz));
                    }
                }
            }
        }
    }

    private void damageOrDestroyBlock(ServerLevel level, BlockPos pos) {
        if (safe) return;

        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;

        float hardness = state.getDestroySpeed(level, pos);

        if (state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)
                || (hardness >= 0F && hardness <= 0.3F)) {
            level.removeBlock(pos, false);
        } else {
            if (hardness < 0F || hardness > 5F) return;

            if (random.nextInt(6) == 1) {
                if (state.is(BlockTags.DIRT)) {
                    level.setBlockAndUpdate(pos, ModBlocks.DIRT_DEAD.get().defaultBlockState());
                } else if (state.is(BlockTags.SAND)) {
                    level.setBlockAndUpdate(
                            pos,
                            random.nextInt(2) == 1
                                    ? Blocks.SANDSTONE.defaultBlockState()
                                    : Blocks.GLASS.defaultBlockState());
                } else if (state.is(BlockTags.BASE_STONE_OVERWORLD)) {
                    level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
                } else if (state.is(BlockTags.GRASS_BLOCKS)) {
                    level.setBlockAndUpdate(pos, ModBlocks.WASTE_EARTH.get().defaultBlockState());
                }
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (amount >= 250F && DamageCategory.of(source) == DamageCategory.EN) discard();
        return false;
    }

    @Override
    public String getRadarName() {
        return "radar.target.meteor";
    }

    @Override
    public RadarTargetType getTargetType() {
        return RadarTargetType.SPECIAL;
    }

    @Override
    public boolean canBeSeenBy(BlockEntity observer) {
        return true;
    }

    @Override
    public boolean matchesScan(RadarScanParams params) {
        return params.scanMissiles();
    }

    @Override
    public boolean suppliesRedstone(RadarScanParams params) {
        return params.scanMissiles();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.safe = input.getBooleanOr("safe", false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putBoolean("safe", safe);
    }
}
