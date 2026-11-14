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

package com.hbm.explosion;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.blocks.ModBlocks;
import com.hbm.platform.Caps;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;

public final class ElectromagneticPulse {

    private static final Direction[] FACES = Direction.values();

    private ElectromagneticPulse() {}

    public static void sphere(Level level, BlockPos centre, int radius) {
        var server = (ServerLevel) level;
        int reach = radius * radius / 2;
        int cx = centre.getX(), cy = centre.getY(), cz = centre.getZ();
        var hit = new ArrayList<BlockPos>();
        for (int chunkX = (cx - radius) >> 4; chunkX <= (cx + radius - 1) >> 4; chunkX++) {
            for (int chunkZ = (cz - radius) >> 4; chunkZ <= (cz + radius - 1) >> 4; chunkZ++) {
                var chunk = server.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                hit.clear();
                for (var pos : chunk.getBlockEntitiesPos()) {
                    int dx = pos.getX() - cx, dy = pos.getY() - cy, dz = pos.getZ() - cz;
                    if (dx * dx + dy * dy + dz * dz < reach) hit.add(pos);
                }
                for (var pos : hit) {
                    var entity = chunk.getBlockEntity(pos);
                    if (entity != null) pulse(server, pos, entity);
                }
            }
        }
    }

    private static void pulse(ServerLevel level, BlockPos pos, BlockEntity entity) {
        var random = level.getRandom();
        if (entity instanceof EnergyHandler endpoint) {
            if (endpoint.getPower() != 0L) {
                endpoint.setPower(0L);
                endpoint.onEnergyChanged();
            }
            if (random.nextInt(5) == 0) scrap(level, pos);
            return;
        }
        if (!offersForeignEnergy(level, pos)) return;
        if (drainForeign(level, pos) && random.nextInt(5) <= 1) scrap(level, pos);
    }

    private static void scrap(ServerLevel level, BlockPos pos) {
        level.setBlock(
                pos, ModBlocks.BLOCK_ELECTRICAL_SCRAP.get().defaultBlockState(), Block.UPDATE_ALL);
    }

    private static boolean offersForeignEnergy(Level level, BlockPos pos) {
        for (var face : FACES) {
            var store = Capabilities.Energy.BLOCK.getCapability(level, pos, null, null, face);
            if (store != null && store.getAmountAsLong() > 0L) return true;
        }
        return false;
    }

    public static boolean holdsEnergy(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof EnergyHandler) return true;
        for (var face : FACES) {
            if (Capabilities.Energy.BLOCK.getCapability(level, pos, null, null, face) != null)
                return true;
        }
        return false;
    }

    public static boolean drainForeign(Level level, BlockPos pos) {
        boolean drained = false;
        for (var face : FACES) {
            var store = Capabilities.Energy.BLOCK.getCapability(level, pos, null, null, face);
            if (store == null) continue;
            var stored = store.getAmountAsLong();
            if (Caps.transfer(store, stored, 1L, false, false) > 0L) drained = true;
        }
        return drained;
    }
}
