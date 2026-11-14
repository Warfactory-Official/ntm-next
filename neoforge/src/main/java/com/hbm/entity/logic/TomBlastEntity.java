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

package com.hbm.entity.logic;

import com.hbm.NuclearTech;
import com.hbm.config.BombConfig;
import com.hbm.config.HbmConfig;
import com.hbm.config.RuntimeConfig;
import com.hbm.explosion.ExplosionTom;
import com.hbm.explosion.NuclearShockwave;
import com.hbm.handler.threading.BombForkJoinPool;
import com.hbm.saveddata.TomSaveData;
import com.hbm.sound.WorldSounds;
import com.hbm.util.ChunkRequests;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class TomBlastEntity extends ExplosionChunkloadingEntity
        implements BombForkJoinPool.IJobCancellable {

    public int age = 0;
    public int destructionRange = 0;
    public boolean did = false;
    private int terrain;
    private long seed;
    private @Nullable ExplosionTom exp;
    private @Nullable ChunkRequests groundRequests;

    public TomBlastEntity(EntityType<? extends TomBlastEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        age = input.getIntOr("age", 0);
        destructionRange = input.getIntOr("destructionRange", 0);
        did = input.getBooleanOr("did", false);
        terrain = input.getIntOr("terrain", ExplosionTom.LEGACY_TERRAIN);
        seed = input.getLongOr("seed", 0L);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("age", age);
        output.putInt("destructionRange", destructionRange);
        output.putBoolean("did", did);
        output.putInt("terrain", terrain);
        output.putLong("seed", seed);
    }

    @Override
    public void cancelJob() {
        if (!(level() instanceof ServerLevel server)) return;
        if (server.getServer().isSameThread()) discard();
        else
            server.getServer()
                    .execute(
                            () -> {
                                if (!isRemoved()) discard();
                            });
    }

    @Override
    public void remove(RemovalReason reason) {
        if (exp != null) exp.close();
        if (groundRequests != null) groundRequests.close();
        super.remove(reason);
    }

    private boolean prepareGround(ServerLevel level, long deadline) {
        if (groundRequests == null) {
            groundRequests =
                    new ChunkRequests(
                            level,
                            _ -> {},
                            failure -> {
                                NuclearTech.LOGGER.error(
                                        "TOM ground loading failed at {}",
                                        blockPosition(),
                                        failure);
                                cancelJob();
                            });
            int minX = ((int) getX() - ExplosionTom.GROUND_REACH) >> 4;
            int maxX = ((int) getX() + ExplosionTom.GROUND_REACH) >> 4;
            int minZ = ((int) getZ() - ExplosionTom.GROUND_REACH) >> 4;
            int maxZ = ((int) getZ() + ExplosionTom.GROUND_REACH) >> 4;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) groundRequests.request(ChunkPos.pack(x, z));
            }
        }
        groundRequests.pump(deadline, BombConfig.chunksInFlight());
        return groundRequests.pendingCount() == 0;
    }

    @Override
    public void tick() {
        super.tick();

        if (!(level() instanceof ServerLevel server)) return;
        long deadline = System.nanoTime() + BombConfig.mk5 * 1_000_000L;

        if (!this.did) {
            if (!prepareGround(server, deadline)) {
                age++;
                return;
            }
            if (((RuntimeConfig) HbmConfig.RUNTIME).extendedLogging())
                NuclearTech.LOGGER.info(
                        "[NUKE] Initialized TOM explosion at {} / {} / {} with strength {}!",
                        getX(),
                        getY(),
                        getZ(),
                        destructionRange);
            terrain = ExplosionTom.groundLevel(server, (int) getX(), (int) getZ());
            seed = random.nextLong();
            this.did = true;
            groundRequests.close();
            groundRequests = null;
        }
        if (exp == null) {
            exp =
                    new ExplosionTom(
                            server,
                            (int) getX(),
                            (int) getZ(),
                            destructionRange,
                            terrain,
                            seed,
                            this);
        }

        boolean flag = exp.tick(deadline);
        if (flag) {
            this.discard();
            TomSaveData data = TomSaveData.get(server);
            data.impact = true;
            data.fire = 1F;
            data.setDirty();
        }

        if (this.random.nextInt(5) == 0)
            WorldSounds.play(
                    level(),
                    null,
                    getX(),
                    getY(),
                    getZ(),
                    SoundEvents.GENERIC_EXPLODE,
                    SoundSource.BLOCKS,
                    10000.0F,
                    0.8F + this.random.nextFloat() * 0.2F);

        if (!flag) {
            WorldSounds.play(
                    level(),
                    null,
                    getX(),
                    getY(),
                    getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.WEATHER,
                    10000.0F,
                    0.8F + this.random.nextFloat() * 0.2F);
            NuclearShockwave.sweep(server, getX(), getY(), getZ(), destructionRange * 2.0D);
        }

        age++;
    }
}
