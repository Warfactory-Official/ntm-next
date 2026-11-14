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

package com.hbm.blockentity.machine;

import com.hbm.blockentity.BlockEntityTicks;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.DestroyedZirnoxBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.sound.WorldSounds;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.TickPhase;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class ZirnoxDestroyedBlockEntity extends BlockEntity implements BlockEntityTicks {

    public boolean onFire = true;
    private int waveTimer;

    public ZirnoxDestroyedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ZIRNOX_DESTROYED.get(), pos, state);
    }

    @Override
    public void tickServer() {
        var pos = worldPosition;
        ServerLevel server = (ServerLevel) level;
        RandomSource rand = server.getRandom();
        boolean wasOnFire = onFire;

        radiate(server, pos);

        if (rand.nextInt(5000) == 0) onFire = false;

        if (--waveTimer <= 0) {
            waveTimer = 100 + rand.nextInt(20);
            ((DestroyedZirnoxBlock) getBlockState().getBlock())
                    .footprint(getBlockState().getValue(MultiblockCoreBlock.FACING))
                    .visit(
                            pos,
                            cell -> {
                                BlockPos vent = cell.above();
                                BlockState above = server.getBlockState(vent);
                                if (above.isAir()) {
                                    if (rand.nextInt(10) == 0)
                                        server.setBlockAndUpdate(
                                                vent,
                                                ModBlocks.GAS_MELTDOWN.get().defaultBlockState());
                                } else if (above.is(ModBlocks.FOAM_LAYER.get())
                                        || above.is(ModBlocks.BLOCK_FOAM.get())) {
                                    if (rand.nextInt(25) == 0) onFire = false;
                                }
                                if (rand.nextInt(10) == 0 && server.getBlockState(vent).isAir()) {
                                    server.setBlockAndUpdate(
                                            vent, ModBlocks.GAS_MELTDOWN.get().defaultBlockState());
                                }
                            });
        }

        if (onFire && TickPhase.every(this, 50)) {
            DestroyedZirnoxBlock.flame(server, pos);
            WorldSounds.machine(
                    server,
                    null,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    SoundEvents.FIRE_AMBIENT,
                    SoundSource.BLOCKS,
                    1.0F + rand.nextFloat(),
                    rand.nextFloat() * 0.7F + 0.3F);
        }

        if (onFire != wasOnFire) level.blockEntityChanged(pos);
    }

    private void radiate(ServerLevel level, BlockPos pos) {
        double cx = pos.getX() + 0.5D, cy = pos.getY() + 0.5D, cz = pos.getZ() + 0.5D;
        ContaminationUtil.radiate(
                level,
                cx,
                cy,
                cz,
                100D,
                onFire ? 500000F : 75000F,
                ContaminationUtil.ContaminationType.CREATIVE);

        if (onFire) {
            AABB box = new AABB(cx, cy, cz, cx, cy, cz).inflate(5D);
            List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, box);
            DamageSource fire = level.damageSources().onFire();
            for (LivingEntity e : near) {
                if (e.distanceToSqr(cx, cy, cz) < 25D) e.hurtServer(level, fire, 2F);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        onFire = input.getBooleanOr("onFire", true);
        waveTimer = input.getIntOr("waveTimer", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("onFire", onFire);
        output.putInt("waveTimer", waveTimer);
    }
}
