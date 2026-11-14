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

import com.hbm.explosion.BlockConversionTable;
import com.hbm.explosion.CompositeExplosion;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BurningFOEQEntity extends Projectile {
    public float renderPitch;
    public float renderPitchO;

    public BurningFOEQEntity(EntityType<? extends BurningFOEQEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();

        this.renderPitchO = this.renderPitch;
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        this.xOld = this.xo;
        this.yOld = this.yo;
        this.zOld = this.zo;

        Vec3 motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        if (motion.y > -4D) {
            this.setDeltaMovement(motion.x, motion.y - 0.1D, motion.z);
        }

        this.rotation();

        BlockPos pos = new BlockPos((int) getX(), (int) getY(), (int) getZ());

        if (!level().getBlockState(pos).isAir()) {
            if (!level().isClientSide()) {
                for (int i = 0; i < 25; i++)
                    CompositeExplosion.detonate(
                            level(),
                            getX() + 0.5F + random.nextGaussian() * 5,
                            getY() + 0.5F + random.nextGaussian() * 5,
                            getZ() + 0.5F + random.nextGaussian() * 5,
                            10.0F,
                            random.nextBoolean() ? CompositeExplosion.CLOUD : 0);
                BlockConversionTable.FALLOUT.sphere(level(), pos, 35, random);
            }
            this.discard();
        }
    }

    public void rotation() {
        Vec3 motion = this.getDeltaMovement();
        float f2 = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        this.setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI));

        this.renderPitch = (float) (Math.atan2(motion.y, f2) * 180.0D / Math.PI) - 90F;

        for (; this.renderPitch - this.renderPitchO < -180.0F; this.renderPitchO -= 360.0F)
            ;
        while (this.renderPitch - this.renderPitchO >= 180.0F) this.renderPitchO += 360.0F;

        while (this.getYRot() - this.yRotO < -180.0F) this.yRotO -= 360.0F;
        while (this.getYRot() - this.yRotO >= 180.0F) this.yRotO += 360.0F;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 100000;
    }
}
