// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.capability.NtmContracts;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;
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

public class CrucibleUtil {

    public static @Nullable MaterialStack pourSingleStack(
            Level level,
            double x,
            double y,
            double z,
            double range,
            boolean safe,
            MaterialStack stack,
            int quanta,
            ImpactPos impact) {
        BlockHitResult hit = traceDown(level, x, y, z, y - range);
        ICrucibleAcceptor acc = getPouringTarget(level, hit);

        if (acc == null) {
            spill(hit, safe, stack, quanta, impact);
            return stack;
        }

        MaterialStack ret = tryPourStack(level, acc, hit, stack, impact);
        if (ret != null) return ret;

        spill(hit, safe, stack, quanta, impact);
        return stack;
    }

    public static @Nullable MaterialStack pourFullStack(
            Level level,
            double x,
            double y,
            double z,
            double range,
            boolean safe,
            List<MaterialStack> stacks,
            int quanta,
            ImpactPos impact) {
        if (stacks.isEmpty()) return null;

        BlockHitResult hit = traceDown(level, x, y, z, y - range);
        ICrucibleAcceptor acc = getPouringTarget(level, hit);

        if (acc == null) {
            return spill(hit, safe, stacks, quanta, impact);
        }

        for (MaterialStack stack : stacks) {
            if (stack.material == null) continue;

            int amountToPour = Math.min(stack.amount, quanta);
            MaterialStack toPour = new MaterialStack(stack.material, amountToPour);
            MaterialStack left = tryPourStack(level, acc, hit, toPour, impact);

            if (left != null) {
                stack.amount -= (amountToPour - left.amount);
                return new MaterialStack(stack.material, stack.amount - left.amount);
            }
        }

        return spill(hit, safe, stacks, quanta, impact);
    }

    public static @Nullable MaterialStack tryPourStack(
            Level level,
            ICrucibleAcceptor acc,
            BlockHitResult hit,
            MaterialStack stack,
            ImpactPos impact) {
        if (stack.material.smeltable != SmeltingBehavior.SMELTABLE) return null;

        Vec3 loc = hit.getLocation();
        BlockPos pos = hit.getBlockPos();

        if (acc.canAcceptPartialPour(level, pos, loc, hit.getDirection(), stack)) {
            MaterialStack left = acc.pour(level, pos, loc, hit.getDirection(), stack);
            if (left == null) left = new MaterialStack(stack.material, 0);

            impact.x = loc.x;
            impact.y = loc.y;
            impact.z = loc.z;
            return left;
        }

        return null;
    }

    public static @Nullable ICrucibleAcceptor getPouringTarget(Level level, BlockHitResult hit) {
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

    public static @Nullable MaterialStack spill(
            BlockHitResult hit,
            boolean safe,
            List<MaterialStack> stacks,
            int quanta,
            ImpactPos impact) {
        MaterialStack top = stacks.get(0);
        MaterialStack ret = spill(hit, safe, top, quanta, impact);
        stacks.removeIf(o -> o.amount <= 0);
        return ret;
    }

    public static @Nullable MaterialStack spill(
            BlockHitResult hit, boolean safe, MaterialStack stack, int quanta, ImpactPos impact) {
        if (safe) return null;

        MaterialStack toWaste = new MaterialStack(stack.material, Math.min(stack.amount, quanta));
        stack.amount -= toWaste.amount;

        if (impact != null && hit != null) {
            Vec3 loc = hit.getLocation();
            impact.x = loc.x;
            impact.y = loc.y;
            impact.z = loc.z;
        }

        return toWaste;
    }

    public static final class ImpactPos {
        public double x, y, z;
    }
}
