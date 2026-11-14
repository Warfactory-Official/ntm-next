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

import com.hbm.entity.projectile.RubbleEntity;
import com.hbm.entity.projectile.ShrapnelEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class BlastDebris {
    private static final float JOLT_RESISTANCE = 70F;

    private BlastDebris() {}

    public static void rubble(Level level, double x, double y, double z, int count) {
        var server = (ServerLevel) level;
        if (count <= 0) return;
        var random = server.getRandom();
        double spread = 0.75D * (1 + count / 50);
        for (int i = 0; i < count; i++) {
            var rubble = new RubbleEntity(server, x, y, z);
            rubble.setDeltaMovement(
                    spread * random.nextGaussian(),
                    0.75D * (1 + (count + random.nextInt(5 * count)) / 25),
                    spread * random.nextGaussian());
            server.addFreshEntity(rubble);
        }
    }

    public static void shrapnel(Level level, double x, double y, double z, int count) {
        burst((ServerLevel) level, x, y, z, count, 1D, false);
    }

    public static void tracers(Level level, double x, double y, double z, int count) {
        burst((ServerLevel) level, x, y, z, count, 0.25D, true);
    }

    private static void burst(
            ServerLevel level,
            double x,
            double y,
            double z,
            int count,
            double scale,
            boolean trailing) {
        var random = level.getRandom();
        double spread = scale * (1 + count / 50);
        for (int i = 0; i < count; i++) {
            var shard = new ShrapnelEntity(level, x, y, z);
            double rise =
                    (random.nextFloat() * 0.5D + 0.5D) * (1 + count / (15 + random.nextInt(21)))
                            + scale * (random.nextFloat() / 50 * count);
            shard.setDeltaMovement(
                    spread * random.nextGaussian(), rise, spread * random.nextGaussian());
            if (trailing || random.nextInt(3) == 0) shard.setTrail(ShrapnelEntity.TRAIL_FLAME);
            level.addFreshEntity(shard);
        }
    }

    public static void shower(
            Level level,
            double x,
            double y,
            double z,
            double mx,
            double my,
            double mz,
            int count,
            double spread) {
        var server = (ServerLevel) level;
        var random = server.getRandom();
        for (int i = 0; i < count; i++) {
            var shard = new ShrapnelEntity(server, x, y, z);
            shard.setDeltaMovement(
                    mx + spread * random.nextGaussian(),
                    my + spread * random.nextGaussian(),
                    mz + spread * random.nextGaussian());
            if (random.nextInt(3) == 0) shard.setTrail(ShrapnelEntity.TRAIL_FLAME);
            server.addFreshEntity(shard);
        }
    }

    public static void missileDebris(
            Level level,
            double x,
            double y,
            double z,
            double mx,
            double my,
            double mz,
            double spread,
            List<ItemStack> parts,
            ItemStack rare) {
        var server = (ServerLevel) level;
        var random = server.getRandom();
        for (var part : parts) {
            if (part.isEmpty()) continue;
            for (int k = random.nextInt(part.getCount() + 1); k > 0; k--) {
                double wx = 0.85D * (mx + spread * random.nextGaussian());
                double wy = 0.85D * (my + spread * random.nextGaussian());
                double wz = 0.85D * (mz + spread * random.nextGaussian());
                server.addFreshEntity(
                        new ItemEntity(
                                server,
                                x + 2 * wx,
                                y + 2 * wy,
                                z + 2 * wz,
                                part.copy(),
                                wx,
                                wy,
                                wz));
            }
        }
        if (!rare.isEmpty() && random.nextInt(10) == 0) {
            double near = 0.1D * spread;
            server.addFreshEntity(
                    new ItemEntity(
                            server,
                            x,
                            y,
                            z,
                            rare.copy(),
                            mx + near * random.nextGaussian(),
                            my + near * random.nextGaussian(),
                            mz + near * random.nextGaussian()));
        }
    }

    public static void jolt(
            Level level, double x, double y, double z, double reach, int rays, double speed) {
        var server = (ServerLevel) level;
        var random = server.getRandom();
        var cursor = new BlockPos.MutableBlockPos();
        var air = Blocks.AIR.defaultBlockState();
        for (int ray = 0; ray < rays; ray++) {
            double azimuth = random.nextDouble() * 2D * Math.PI;
            double cos = random.nextDouble() * 2D - 1D;
            double sin = Math.sqrt(1D - cos * cos);
            double ux = sin * Math.cos(azimuth), uy = cos, uz = sin * Math.sin(azimuth);
            for (int i = 0; i < reach; i++) {
                double qx = x + i * ux, qy = y + i * uy, qz = z + i * uz;
                var state =
                        server.getBlockState(
                                cursor.set(Mth.floor(qx), Mth.floor(qy), Mth.floor(qz)));
                if (state.liquid()) {
                    server.setBlock(cursor, air, Block.UPDATE_ALL);
                    continue;
                }
                if (state.isAir() || state.getBlock().getExplosionResistance() > JOLT_RESISTANCE)
                    continue;
                double rx = qx + 0.5D, ry = qy + 0.5D, rz = qz + 0.5D;
                var rubble = new RubbleEntity(server, rx, ry, rz);
                rubble.setBlockState(state);
                rubble.setDeltaMovement(speed * (x - rx), speed * (y - ry), speed * (z - rz));
                server.addFreshEntity(rubble);
                server.setBlock(cursor, air, Block.UPDATE_ALL);
                break;
            }
        }
    }
}
