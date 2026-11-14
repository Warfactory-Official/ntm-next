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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class CoinEntity extends InterpolatedProjectileEntity {

    public CoinEntity(EntityType<? extends CoinEntity> type, Level level) {
        super(type, level);
    }

    public CoinEntity(Level level) {
        this(ModEntities.COIN.get(), level);
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        return super.makeBoundingBox(position.subtract(0D, getBbHeight() / 2D, 0D));
    }

    @Override
    public boolean doesImpactEntities() {
        return false;
    }

    @Override
    protected void onImpact(HitResult mop) {
        if (mop instanceof BlockHitResult) this.discard();
    }

    @Override
    protected float getAirDrag() {
        return 1F;
    }

    @Override
    public double getGravityVelocity() {
        return 0.02D;
    }

    @Override
    public boolean isPickable() {
        return true;
    }
}
