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

package com.hbm.entity.projectile;

import com.hbm.entity.ModEntities;
import com.hbm.lib.ModDamageTypes;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class RubbleEntity extends ThrowableProjectile {

    private static final EntityDataAccessor<Integer> STATE_ID =
            SynchedEntityData.defineId(RubbleEntity.class, EntityDataSerializers.INT);

    public RubbleEntity(EntityType<? extends RubbleEntity> type, Level level) {
        super(type, level);
    }

    public RubbleEntity(Level level, double x, double y, double z) {
        this(ModEntities.RUBBLE.get(), level);
        setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STATE_ID, Block.getId(Blocks.STONE.defaultBlockState()));
    }

    public BlockState getBlockState() {
        return Block.stateById(entityData.get(STATE_ID));
    }

    public void setBlockState(BlockState state) {
        entityData.set(STATE_ID, Block.getId(state));
    }

    @Override
    protected float getAirDrag() {
        return 1F;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.03;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level() instanceof ServerLevel server) {
            result.getEntity()
                    .hurtServer(server, damageSources().source(ModDamageTypes.RUBBLE), 15F);
        }
        finishImpact();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        finishImpact();
    }

    private void finishImpact() {
        if (level() instanceof ServerLevel server && tickCount > 2) {
            WorldSounds.play(
                    server,
                    null,
                    getX(),
                    getY(),
                    getZ(),
                    ModSounds.BLOCK_DEBRIS.get(),
                    SoundSource.BLOCKS,
                    1.5F,
                    1.0F);
            server.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, getBlockState()),
                    getX(),
                    getY(),
                    getZ(),
                    64,
                    0.25D,
                    0.25D,
                    0.25D,
                    0.0D);
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("BlockState", BlockState.CODEC, getBlockState());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("BlockState", BlockState.CODEC).ifPresent(this::setBlockState);
    }
}
