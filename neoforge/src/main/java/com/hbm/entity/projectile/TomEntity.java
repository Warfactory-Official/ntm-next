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
import com.hbm.entity.effect.CloudTomEntity;
import com.hbm.entity.logic.TomBlastEntity;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.ChunkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class TomEntity extends Projectile {

    public TomEntity(EntityType<? extends TomEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();

        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        ChunkUtil.holdOwnChunk(this);

        if (this.tickCount % 100 == 0) {
            WorldSounds.play(
                    level(),
                    null,
                    getX(),
                    getY(),
                    getZ(),
                    ModSounds.ALARM_CHIME.get(),
                    SoundSource.RECORDS,
                    10000.0F,
                    1.0F);
        }

        setDeltaMovement(motion.x, -0.5D, motion.z);

        if (!level().getBlockState(new BlockPos((int) getX(), (int) getY(), (int) getZ())).isAir()
                || getY() < 10) {
            if (!level().isClientSide()) {

                TomBlastEntity tom = new TomBlastEntity(ModEntities.TOM_BUST.get(), level());
                tom.setPos(getX(), getY(), getZ());
                tom.destructionRange = 600;
                level().addFreshEntity(tom);

                level().addFreshEntity(
                                CloudTomEntity.statFac(level(), 500, getX(), getY(), getZ()));
            }
            this.discard();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000D;
    }
}
