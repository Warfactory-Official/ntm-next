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

package com.hbm.blockentity.bomb;

import com.hbm.blockentity.BlockEntityTicks;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.bomb.APLandmineBlock;
import com.hbm.blocks.bomb.LandmineBlock;
import com.hbm.interfaces.TriggerResult;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.stats.ModStats;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public final class LandmineBlockEntity extends BlockEntity implements BlockEntityTicks {
    private boolean primed;
    private boolean waiting;

    public LandmineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LANDMINE.get(), pos, state);
    }

    public void deferUntilPlayerNear() {
        if (!waiting) {
            waiting = true;
            setChanged();
        }
    }

    @Override
    public void tickServer() {
        var state = getBlockState();
        if (!(level instanceof ServerLevel serverLevel)
                || !(state.getBlock() instanceof LandmineBlock host)) {
            return;
        }

        if (host instanceof APLandmineBlock) {
            APLandmineBlock.refreshGround(serverLevel, worldPosition, state);
        }
        if (!serverLevel.getBlockState(worldPosition.above()).isAir()) {
            return;
        }

        var range = host.range;
        var height = host.height;
        if (this.waiting) {
            range = 25;
            height = 25;
        } else if (!this.primed) {
            range *= 2;
            height *= 2;
        }

        var x = worldPosition.getX();
        var y = worldPosition.getY();
        var z = worldPosition.getZ();
        var bounds =
                new AABB(
                        x - range, y - height, z - range, x + range + 1, y + height, z + range + 1);
        var entities = serverLevel.getEntities(null, bounds);
        for (var index = 0; index < entities.size(); index++) {
            var entity = entities.get(index);
            var category = entity.getType().getCategory();
            if (category == MobCategory.WATER_CREATURE || category == MobCategory.AMBIENT) {
                continue;
            }

            if (this.waiting) {
                if (entity instanceof Player) {
                    this.waiting = false;
                    this.setChanged();
                    return;
                }
                continue;
            }

            if (!(entity instanceof LivingEntity)) {
                continue;
            }
            if (!this.primed) {
                return;
            }

            var outcome = host.trigger(serverLevel, worldPosition, entity);
            if (outcome == TriggerResult.DETONATED && entity instanceof Player player) {
                player.awardStat(ModStats.MINES.get());
            }
            return;
        }

        if (!this.waiting && !this.primed) {
            playArmingCue(serverLevel, worldPosition);
            this.primed = true;
            this.setChanged();
        }
    }

    private static void playArmingCue(ServerLevel level, BlockPos pos) {
        WorldSounds.machine(
                level,
                null,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                ModSounds.FSTBMB_START.get(),
                SoundSource.BLOCKS,
                3F,
                1F);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("primed", primed);
        output.putBoolean("waiting", waiting);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        primed = input.getBooleanOr("primed", false);
        waiting = input.getBooleanOr("waiting", false);
    }
}
