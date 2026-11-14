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

package com.hbm.entity.particle;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.PinkCloudBroadcasterBlock;
import com.hbm.blocks.machine.RadioRecBlock;
import com.hbm.entity.ModEntities;
import com.hbm.explosion.ExplosionChaos;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class PinkCloudFXEntity extends Entity {

    private int particleAge;
    private int maxAge;

    public PinkCloudFXEntity(EntityType<? extends PinkCloudFXEntity> type, Level level) {
        super(type, level);
        noPhysics = false;
    }

    public PinkCloudFXEntity(
            Level level,
            double x,
            double y,
            double z,
            double motionX,
            double motionY,
            double motionZ) {
        this(ModEntities.PINK_CLOUD_FX.get(), level);
        setPos(x, y, z);
        setDeltaMovement(ModFXMotion.spawn(motionX, motionY, motionZ));
    }

    public int particleAge() {
        return particleAge;
    }

    public int maxAge() {
        return maxAge;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000D;
    }

    @Override
    public void tick() {
        xo = getX();
        yo = getY();
        zo = getZ();
        super.tick();

        if (maxAge < 900) maxAge = random.nextInt(301) + 900;

        if (!level().isClientSide() && random.nextInt(50) == 0)
            ExplosionChaos.pinkCloud(level(), getX(), getY(), getZ(), 2D);

        if (++particleAge >= maxAge) {
            discard();
            return;
        }

        double motionX = getDeltaMovement().x * 0.76D;
        double motionY = getDeltaMovement().y * 0.76D;
        double motionZ = getDeltaMovement().z * 0.76D;

        if (onGround()) {
            motionX *= 0.7D;
            motionZ *= 0.7D;
        }

        if (level().isRaining() && level().canSeeSky(blockPosition())) motionY -= 0.01D;

        setDeltaMovement(motionX, motionY, motionZ);

        for (int i = 0; i < 4; i++) {
            setPos(getX() + motionX / 4D, getY() + motionY / 4D, getZ() + motionZ / 4D);

            BlockPos pos = new BlockPos((int) getX(), (int) getY(), (int) getZ());
            var state = level().getBlockState(pos);
            if (level() instanceof ServerLevel server && state.is(ModBlocks.RADIO_REC.get())) {
                discard();
                state =
                        ModBlocks.BROADCASTER_PC
                                .get()
                                .defaultBlockState()
                                .setValue(
                                        PinkCloudBroadcasterBlock.FACING,
                                        state.getValue(RadioRecBlock.FACING));
                server.setBlock(pos, state, Block.UPDATE_CLIENTS);
            }
            if (!state.isSolidRender()) continue;

            if (!level().isClientSide() && random.nextInt(5) != 0) discard();

            setPos(getX() - motionX / 4D, getY() - motionY / 4D, getZ() - motionZ / 4D);
            setDeltaMovement(0D, 0D, 0D);
            motionX = motionY = motionZ = 0D;
        }
    }
}
