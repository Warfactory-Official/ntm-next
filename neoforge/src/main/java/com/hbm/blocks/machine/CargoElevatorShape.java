// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.ArrayVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraft.world.phys.shapes.OffsetDoubleList;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class CargoElevatorShape extends ArrayVoxelShape {
    private final AABB[] boxes;
    private final int height;
    private final double coreX;
    private final double coreY;
    private final double coreZ;

    public CargoElevatorShape(VoxelShape geometry, AABB[] boxes, int height) {
        this(
                geometry.shape,
                geometry.getCoords(Direction.Axis.X),
                geometry.getCoords(Direction.Axis.Y),
                geometry.getCoords(Direction.Axis.Z),
                boxes,
                height,
                0,
                0,
                0);
    }

    private CargoElevatorShape(
            DiscreteVoxelShape geometry,
            DoubleList xs,
            DoubleList ys,
            DoubleList zs,
            AABB[] boxes,
            int height,
            double coreX,
            double coreY,
            double coreZ) {
        super(geometry, xs, ys, zs);
        this.boxes = boxes;
        this.height = height;
        this.coreX = coreX;
        this.coreY = coreY;
        this.coreZ = coreZ;
    }

    @Override
    public VoxelShape move(double dx, double dy, double dz) {
        if (dx == 0 && dy == 0 && dz == 0) return this;
        return new CargoElevatorShape(
                shape,
                new OffsetDoubleList(getCoords(Direction.Axis.X), dx),
                new OffsetDoubleList(getCoords(Direction.Axis.Y), dy),
                new OffsetDoubleList(getCoords(Direction.Axis.Z), dz),
                boxes,
                height,
                coreX + dx,
                coreY + dy,
                coreZ + dz);
    }

    @Override
    public VoxelShape optimize() {
        return this;
    }

    @Override
    public @Nullable BlockHitResult clip(Vec3 from, Vec3 to, BlockPos pos) {
        double x = pos.getX() + coreX, y = pos.getY() + coreY, z = pos.getZ() + coreZ;
        Vec3 start = from.subtract(x, y, z);
        Vec3 end = to.subtract(x, y, z);

        for (AABB box : boxes) {
            BlockHitResult hit = clipBox(box, start, end, pos);
            if (hit != null) {
                Vec3 point = hit.getLocation().add(x, y, z);

                BlockPos surface =
                        BlockPos.containing(
                                Math.clamp(point.x, x - 1D, Math.nextDown(x + 2D)),
                                Math.clamp(point.y, y, Math.nextDown(y + height + 1D)),
                                Math.clamp(point.z, z - 1D, Math.nextDown(z + 2D)));
                return new BlockHitResult(point, hit.getDirection(), surface, false);
            }
        }
        return null;
    }

    private static @Nullable BlockHitResult clipBox(AABB box, Vec3 from, Vec3 to, BlockPos cell) {
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        Vec3 best = null;
        Direction face = Direction.WEST;
        double distance = Double.POSITIVE_INFINITY;
        for (int axis = 0; axis < 3; axis++) {
            double delta = axis == 0 ? dx : axis == 1 ? dy : dz;

            if (delta * delta < 1.0E-7F) continue;
            double start = axis == 0 ? from.x : axis == 1 ? from.y : from.z;
            for (int side = 0; side < 2; side++) {
                double plane =
                        axis == 0
                                ? side == 0 ? box.minX : box.maxX
                                : axis == 1
                                        ? side == 0 ? box.minY : box.maxY
                                        : side == 0 ? box.minZ : box.maxZ;
                double t = (plane - start) / delta;
                if (t < 0 || t > 1) continue;
                Vec3 point = from.add(dx * t, dy * t, dz * t);
                if (axis != 0 && (point.x < box.minX || point.x > box.maxX)
                        || axis != 1 && (point.y < box.minY || point.y > box.maxY)
                        || axis != 2 && (point.z < box.minZ || point.z > box.maxZ)) continue;
                double candidate = from.distanceToSqr(point);
                if (candidate < distance) {
                    best = point;
                    distance = candidate;
                    face =
                            axis == 0
                                    ? side == 0 ? Direction.WEST : Direction.EAST
                                    : axis == 1
                                            ? side == 0 ? Direction.DOWN : Direction.UP
                                            : side == 0 ? Direction.NORTH : Direction.SOUTH;
                }
            }
        }
        return best == null ? null : new BlockHitResult(best, face, cell, false);
    }
}
