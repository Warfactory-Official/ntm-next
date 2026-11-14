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

package com.hbm.entity.grenade;

import com.hbm.NuclearTech;
import com.hbm.config.HbmConfig;
import com.hbm.config.RuntimeConfig;
import com.hbm.entity.projectile.ProjectileEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

@Deprecated
public abstract class GrenadeBaseEntity extends ProjectileEntity {

    public GrenadeBaseEntity(EntityType<? extends GrenadeBaseEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;

        xRotO = getXRot();
        setXRot(getXRot() - (float) (getDeltaMovement().length() * 25));
    }

    @Override
    protected void onImpact(HitResult mop) {
        if (mop instanceof EntityHitResult entityHit && level() instanceof ServerLevel server) {
            Entity hit = entityHit.getEntity();
            hit.hurtServer(server, damageSources().thrown(this, getOwner()), 0F);
        }

        if (!level().isClientSide() && ((RuntimeConfig) HbmConfig.RUNTIME).extendedLogging()) {
            Entity thrower = getOwner();
            NuclearTech.LOGGER.info(
                    "[GREN] Set off grenade at {} / {} / {} by {}!",
                    (int) getX(),
                    (int) getY(),
                    (int) getZ(),
                    thrower instanceof Player player
                            ? player.getDisplayName().getString()
                            : "null");
        }

        explode();
    }

    public abstract void explode();
}
