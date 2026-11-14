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
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;

public class ChopperMineEntity extends ProjectileEntity {
    public int timer;

    public ChopperMineEntity(EntityType<? extends ChopperMineEntity> type, Level level) {
        super(type, level);
    }

    public ChopperMineEntity(
            Level level,
            double x,
            double y,
            double z,
            double mx,
            double my,
            double mz,
            Entity shooter) {
        this(ModEntities.CHOPPER_MINE.get(), level);
        setOwner(shooter);
        setPos(x, y, z);
        setDeltaMovement(mx, my, mz);
    }

    @Override
    public boolean isSpectral() {
        return true;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return entity instanceof Player && super.canHitEntity(entity);
    }

    @Override
    public double getGravityVelocity() {
        return getDeltaMovement().y > -0.85 ? 0.05 : 0;
    }

    @Override
    protected float getAirDrag() {
        return 1;
    }

    @Override
    public float getWaterDrag() {
        return 1;
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel server) {
            if (timer >= 100 || !server.getBlockState(blockPosition()).isAir()) {
                detonate(server);
                return;
            }
            WorldSounds.play(
                    server,
                    null,
                    getX(),
                    getY(),
                    getZ(),
                    ModSounds.NULL_MINE.get(),
                    SoundSource.PLAYERS,
                    10,
                    1);
        }
        setDeltaMovement(getDeltaMovement().multiply(0.9, 1, 0.9));
        super.tick();
        if (!isRemoved()) ++timer;
    }

    @Override
    protected void onImpact(HitResult result) {
        if (level() instanceof ServerLevel server) detonate(server);
    }

    private void detonate(ServerLevel server) {
        discard();
        explode(server);
    }

    protected void explode(ServerLevel server) {
        server.explode(
                getOwner(),
                null,
                null,
                getX(),
                getY(),
                getZ(),
                5,
                false,
                Level.ExplosionInteraction.NONE);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Timer", timer);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        timer = input.getIntOr("Timer", 0);
    }
}
