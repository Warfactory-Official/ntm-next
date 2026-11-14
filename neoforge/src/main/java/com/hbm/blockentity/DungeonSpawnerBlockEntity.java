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

package com.hbm.blockentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.UndeadSoldierEntity;
import com.hbm.items.ModItems;
import com.hbm.items.variant.SecretPartType;
import com.hbm.platform.Platform;
import com.hbm.util.TickPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public final class DungeonSpawnerBlockEntity extends BlockEntity implements BlockEntityTicks {

    private static final int WAVES = 2;
    private static final int SOLDIERS = 10;
    private static final int WAVE_GRACE = 60;
    private static final int CHECK_PERIOD = 20;
    private static final int HOLDER_HEIGHT = 18;
    private static final double[][] RING = new double[SOLDIERS][];

    static {
        double x = 10;
        double z = 0;
        double radians = 36D / 180D * Math.PI;
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        for (int i = 0; i < SOLDIERS; i++) {
            RING[i] = new double[] {x, z};
            double turned = x * cos + z * sin;
            z = z * cos - x * sin;
            x = turned;
        }
    }

    private int phase;
    private int timer;

    public DungeonSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NTM_DUNGEON_SPAWNER.get(), pos, state);
    }

    @Override
    public void tickServer() {
        var level = (ServerLevel) this.level;
        if (phase > WAVES) leaveReward(level);
        else if (phase > 0 && timer == 0) callWave(level);

        if (readyToAdvance(level)) {
            phase++;
            timer = 0;
            level.blockEntityChanged(worldPosition);
        } else {
            timer++;
            if (timer <= WAVE_GRACE) level.blockEntityChanged(worldPosition);
        }
    }

    private boolean readyToAdvance(ServerLevel level) {
        if (level.getDifficulty() == Difficulty.PEACEFUL
                || phase > WAVES
                || !TickPhase.every(this, CHECK_PERIOD)) {
            return false;
        }
        if (phase == 0) return !level.getEntitiesOfClass(Player.class, wakeBox()).isEmpty();
        return timer >= WAVE_GRACE
                && level.getEntitiesOfClass(UndeadSoldierEntity.class, waveBox()).isEmpty();
    }

    private AABB wakeBox() {
        return new AABB(
                worldPosition.getX() - 20,
                worldPosition.getY() - 10,
                worldPosition.getZ() - 20,
                worldPosition.getX() + 21,
                worldPosition.getY() + 8,
                worldPosition.getZ() + 21);
    }

    private AABB waveBox() {
        return new AABB(
                worldPosition.getX() - 50,
                worldPosition.getY() - 20,
                worldPosition.getZ() - 50,
                worldPosition.getX() + 48,
                worldPosition.getY() + 21,
                worldPosition.getZ() + 51);
    }

    private void callWave(ServerLevel level) {
        for (int i = 0; i < SOLDIERS; i++) {
            UndeadSoldierEntity soldier =
                    ModEntities.UNDEAD_SOLDIER.get().create(level, EntitySpawnReason.EVENT);
            if (soldier == null) return;
            soldier.snapTo(
                    worldPosition.getX() + 0.5D + RING[i][0],
                    worldPosition.getY() - 5D,
                    worldPosition.getZ() + 0.5D + RING[i][1],
                    i * 36F,
                    0F);
            if (!soldier.canSpawnAtCurrentPosition()) continue;
            Platform.finalizeSpawn(
                    soldier,
                    level,
                    level.getCurrentDifficultyAt(soldier.blockPosition()),
                    EntitySpawnReason.EVENT,
                    null);
            level.addFreshEntity(soldier);
        }
    }

    private void leaveReward(ServerLevel level) {
        BlockPos holderPos = worldPosition.above(HOLDER_HEIGHT);
        if (level.getBlockEntity(holderPos) instanceof SkeletonHolderBlockEntity holder) {
            holder.hbm$syncSet$SkeletonHolderBlockEntity$item(
                    level.getRandom().nextInt(5) == 0
                            ? ModItems.ITEM_SECRET.stack(SecretPartType.ABERRATOR)
                            : new ItemStack(ModItems.CLAY_TABLET_1));
            holder.setChanged();
            BlockState state = level.getBlockState(holderPos);
            level.sendBlockUpdated(holderPos, state, state, Block.UPDATE_CLIENTS);
        }
        level.setBlockAndUpdate(worldPosition, Blocks.OBSIDIAN.defaultBlockState());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("phase", phase);
        output.putInt("timer", timer);
        output.putByte("type", (byte) 0);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        phase = input.getIntOr("phase", 0);
        timer = input.getIntOr("timer", 0);
    }
}
