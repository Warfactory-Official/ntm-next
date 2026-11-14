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

package com.hbm.util;

import com.hbm.api.block.MoltenMetalHandler;
import com.hbm.capability.NtmContracts;
import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.MaterialStack;
import com.hbm.inventory.material.SmeltingBehavior;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class CrucibleUtil {
    public record Pour(Material material, int amount, Vec3 impact) {}

    private CrucibleUtil() {}

    public static @Nullable Pour pourStack(
            Level level, double x, double y, double z, double range, MaterialStack stack) {
        BlockHitResult hit = traceDown(level, x, y, z, y - range);
        MoltenMetalHandler target = getPouringTarget(level, hit);
        return target == null ? null : offer(level, target, hit, stack, Integer.MAX_VALUE);
    }

    public static @Nullable Pour pourFirst(
            Level level,
            double x,
            double y,
            double z,
            double range,
            List<MaterialStack> stacks,
            int quanta) {
        if (stacks.isEmpty()) return null;
        BlockHitResult hit = traceDown(level, x, y, z, y - range);
        MoltenMetalHandler target = getPouringTarget(level, hit);
        if (target == null) return null;
        for (MaterialStack stack : stacks) {
            Pour pour = offer(level, target, hit, stack, quanta);
            if (pour != null) return pour;
        }
        return null;
    }

    private static @Nullable Pour offer(
            Level level,
            MoltenMetalHandler target,
            BlockHitResult hit,
            MaterialStack source,
            int cap) {
        if (source.material == null || source.material.behaviour() != SmeltingBehavior.SMELTABLE)
            return null;
        int size = Math.min(source.amount, cap);
        MaterialStack offered = new MaterialStack(source.material, size);
        BlockPos pos = hit.getBlockPos();
        Vec3 impact = hit.getLocation();
        if (!target.canAcceptPour(level, pos, impact, hit.getDirection(), offered)) return null;
        MaterialStack rest = target.acceptPour(level, pos, impact, hit.getDirection(), offered);
        int taken = size - (rest == null ? 0 : rest.amount);
        source.hbm$syncSet$MaterialStack$amount((int) (source.amount - (taken)));
        return new Pour(source.material, taken, impact);
    }

    public static @Nullable MoltenMetalHandler getPouringTarget(Level level, BlockHitResult hit) {
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        return NtmContracts.CRUCIBLE_ACCEPTOR.at(level, hit.getBlockPos());
    }

    public static BlockHitResult traceDown(
            Level level, double x, double startY, double z, double endY) {
        int x0 = Mth.floor(x), z0 = Mth.floor(z);
        double lx = x - x0, lz = z - z0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int by = Mth.floor(startY); by >= Mth.floor(endY); by--) {
            cursor.set(x0, by, z0);
            BlockState state = level.getBlockState(cursor);

            double bestY = Double.NEGATIVE_INFINITY;
            Direction bestSide = null;

            if (!state.getCollisionShape(level, cursor).isEmpty()) {
                for (AABB box : state.getShape(level, cursor).toAabbs()) {
                    if (lx < box.minX || lx > box.maxX || lz < box.minZ || lz > box.maxZ) continue;
                    double topY = by + box.maxY;
                    double botY = by + box.minY;
                    double hitY;
                    Direction side;
                    if (startY >= topY) {
                        hitY = topY;
                        side = Direction.UP;
                    } else if (startY >= botY) {
                        hitY = botY;
                        side = Direction.DOWN;
                    } else {
                        continue;
                    }
                    if (hitY < endY) continue;
                    if (hitY > bestY) {
                        bestY = hitY;
                        bestSide = side;
                    }
                }
            }

            if (bestSide != null) {
                return new BlockHitResult(
                        new Vec3(x, bestY, z), bestSide, cursor.immutable(), false);
            }
        }

        return BlockHitResult.miss(
                new Vec3(x, endY, z), Direction.DOWN, BlockPos.containing(x, endY, z));
    }
}
