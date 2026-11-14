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

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.explosion.ElectromagneticPulse;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class EMPEntity extends Entity {

    List<int[]> machines;
    int life = 10 * 60 * 20;
    private final BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();

    public EMPEntity(EntityType<? extends EMPEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {

        if (!level().isClientSide()) {
            if (machines == null) {
                allocate();
            } else {
                shock();
            }

            if (this.tickCount > life) this.discard();
        }
    }

    private void allocate() {

        machines = new ArrayList<>();
        int radius = 100;

        int originX = (int) getX(), originY = (int) getY(), originZ = (int) getZ();
        ChunkSource chunks = level().getChunkSource();
        boolean[] chunkLoadedAtZ = new boolean[radius * 2 + 1];

        for (int x = -radius; x <= radius; x++) {
            int x2 = (int) Math.pow(x, 2);

            int chunkX = SectionPos.blockToSectionCoord(originX + x);
            int lastChunkZ = Integer.MIN_VALUE;
            boolean loaded = false;
            for (int z = -radius; z <= radius; z++) {
                int chunkZ = SectionPos.blockToSectionCoord(originZ + z);
                if (chunkZ != lastChunkZ) {
                    lastChunkZ = chunkZ;
                    loaded = chunks.getChunkNow(chunkX, chunkZ) != null;
                }
                chunkLoadedAtZ[z + radius] = loaded;
            }

            for (int y = -radius; y <= radius; y++) {
                int y2 = (int) Math.pow(y, 2);

                for (int z = -radius; z <= radius; z++) {
                    if (!chunkLoadedAtZ[z + radius]) continue;
                    int z2 = (int) Math.pow(z, 2);

                    if (Math.sqrt(x2 + y2 + z2) <= radius) {
                        add(this.scratch.set(originX + x, originY + y, originZ + z));
                    }
                }
            }
        }
    }

    private void shock() {

        for (int i = 0; i < machines.size(); i++) {
            emp(machines.get(i)[0], machines.get(i)[1], machines.get(i)[2]);
        }
    }

    private void add(BlockPos pos) {
        if (ElectromagneticPulse.holdsEnergy(level(), pos)) {
            machines.add(new int[] {pos.getX(), pos.getY(), pos.getZ()});
        }
    }

    private void emp(int x, int y, int z) {

        BlockPos pos = this.scratch.set(x, y, z);
        BlockEntity be = level().getBlockEntity(pos);

        boolean flag = false;

        if (be instanceof EnergyHandler handler) {

            handler.setPower(0);
            level().blockEntityChanged(pos);
            flag = true;
        }

        if (ElectromagneticPulse.drainForeign(level(), pos)) flag = true;

        if (flag && random.nextInt(20) == 0) {
            if (level() instanceof ServerLevel server) {
                server.sendParticles(
                        new BlockParticleOption(
                                ParticleTypes.BLOCK,
                                Blocks.STAINED_GLASS.lightBlue().defaultBlockState()),
                        x + 0.5D,
                        y + 0.5D,
                        z + 0.5D,
                        64,
                        0.25D,
                        0.25D,
                        0.25D,
                        0.0D);
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}
}
