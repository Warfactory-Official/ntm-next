// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.multiblock;

import com.hbm.util.Facing;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class MultiblockHandlerXR {

    private MultiblockHandlerXR() {}

    public static boolean checkSpace(
            Level level, BlockPos origin, int[] dim, BlockPos placed, Direction dir) {
        if (dim == null || dim.length != 6) return false;

        int[] rot = rotate(dim, dir);
        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();
        int px = placed.getX(), py = placed.getY(), pz = placed.getZ();

        BlockPos.MutableBlockPos pos = CoreIndexScratch.boxCursor();
        for (int a = ox - rot[4]; a <= ox + rot[5]; a++) {
            for (int b = oy - rot[1]; b <= oy + rot[0]; b++) {
                for (int c = oz - rot[2]; c <= oz + rot[3]; c++) {
                    if (a == px && b == py && c == pz) continue;
                    if (!level.getBlockState(pos.set(a, b, c)).canBeReplaced()) return false;
                }
            }
        }

        return true;
    }

    public static void visitBox(BlockPos origin, int[] dim, Direction dir, BoxVisitor out) {
        if (dim == null || dim.length != 6) return;

        int[] rot = rotate(dim, dir);
        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();

        BlockPos.MutableBlockPos pos = CoreIndexScratch.boxCursor();
        for (int a = ox - rot[4]; a <= ox + rot[5]; a++) {
            for (int b = oy - rot[1]; b <= oy + rot[0]; b++) {
                for (int c = oz - rot[2]; c <= oz + rot[3]; c++) {
                    Direction facingCore;
                    if (b < oy) facingCore = Direction.DOWN;
                    else if (b > oy) facingCore = Direction.UP;
                    else if (a < ox) facingCore = Direction.WEST;
                    else if (a > ox) facingCore = Direction.EAST;
                    else if (c < oz) facingCore = Direction.NORTH;
                    else if (c > oz) facingCore = Direction.SOUTH;
                    else continue;
                    out.cell(pos.set(a, b, c), facingCore);
                }
            }
        }
    }

    public static void placeAssembled(
            LevelAccessor level, BlockPos core, BlockMultiblockCore block, Direction dir) {
        level.setBlock(
                core, block.defaultBlockState().setValue(BlockMultiblockCore.FACING, dir), 3);
        block.fillSpace(level, core, dir);
    }

    public static int[] rotate(int[] dim, Direction dir) {
        if (dim == null) return null;
        return switch (dir) {
            case NORTH -> new int[] {dim[0], dim[1], dim[3], dim[2], dim[5], dim[4]};
            case EAST -> new int[] {dim[0], dim[1], dim[5], dim[4], dim[2], dim[3]};
            case WEST -> new int[] {dim[0], dim[1], dim[4], dim[5], dim[3], dim[2]};
            default -> dim;
        };
    }

    public static VoxelShape boundingSilhouette(
            List<AABB> bounding, Direction rot, int dx, int dy, int dz) {
        VoxelShape shape = Shapes.empty();
        for (AABB aabb : bounding) {
            shape =
                    Shapes.joinUnoptimized(
                            shape,
                            Shapes.create(
                                    Facing.rotateAabb(aabb, rot).move(dx + 0.5, dy, dz + 0.5)),
                            BooleanOp.OR);
        }
        return shape.optimize();
    }

    @FunctionalInterface
    public interface BoxVisitor {
        void cell(BlockPos pos, Direction facingCore);
    }
}
