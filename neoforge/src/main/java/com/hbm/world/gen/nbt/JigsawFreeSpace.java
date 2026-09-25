// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import java.util.Arrays;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class JigsawFreeSpace {
    private int[] boxes = new int[6 * 16];
    private int count;

    private JigsawFreeSpace() {}

    public static JigsawFreeSpace of(VoxelShape shape) {
        JigsawFreeSpace space = new JigsawFreeSpace();
        for (AABB box : shape.toAabbs()) {
            space.add(
                    lattice(box.minX),
                    lattice(box.minY),
                    lattice(box.minZ),
                    lattice(box.maxX),
                    lattice(box.maxY),
                    lattice(box.maxZ));
        }
        return space;
    }

    private static int lattice(double coordinate) {
        int value = (int) Math.floor(coordinate);
        if (value != coordinate)
            throw new IllegalStateException(
                    "jigsaw free space off the block lattice: " + coordinate);
        return value;
    }

    public boolean contains(BoundingBox box) {
        int x0 = box.minX(), y0 = box.minY(), z0 = box.minZ();
        int x1 = box.maxX() + 1, y1 = box.maxY() + 1, z1 = box.maxZ() + 1;
        long covered = 0;
        for (int i = 0; i < count * 6; i += 6) {
            long dx = Math.min(x1, boxes[i + 3]) - Math.max(x0, boxes[i]);
            if (dx <= 0) continue;
            long dy = Math.min(y1, boxes[i + 4]) - Math.max(y0, boxes[i + 1]);
            if (dy <= 0) continue;
            long dz = Math.min(z1, boxes[i + 5]) - Math.max(z0, boxes[i + 2]);
            if (dz <= 0) continue;
            covered += dx * dy * dz;
        }
        return covered == (long) (x1 - x0) * (y1 - y0) * (z1 - z0);
    }

    public void subtract(BoundingBox box) {
        int x0 = box.minX(), y0 = box.minY(), z0 = box.minZ();
        int x1 = box.maxX() + 1, y1 = box.maxY() + 1, z1 = box.maxZ() + 1;
        int[] old = boxes;
        int oldCount = count;
        boxes = new int[Math.max(old.length, 6 * 16)];
        count = 0;
        for (int i = 0; i < oldCount * 6; i += 6) {
            int bx0 = old[i],
                    by0 = old[i + 1],
                    bz0 = old[i + 2],
                    bx1 = old[i + 3],
                    by1 = old[i + 4],
                    bz1 = old[i + 5];
            if (x1 <= bx0 || bx1 <= x0 || y1 <= by0 || by1 <= y0 || z1 <= bz0 || bz1 <= z0) {
                add(bx0, by0, bz0, bx1, by1, bz1);
                continue;
            }
            int cx0 = Math.max(x0, bx0), cx1 = Math.min(x1, bx1);
            int cy0 = Math.max(y0, by0), cy1 = Math.min(y1, by1);
            add(bx0, by0, bz0, cx0, by1, bz1);
            add(cx1, by0, bz0, bx1, by1, bz1);
            add(cx0, by0, bz0, cx1, cy0, bz1);
            add(cx0, cy1, bz0, cx1, by1, bz1);
            add(cx0, cy0, bz0, cx1, cy1, Math.max(z0, bz0));
            add(cx0, cy0, Math.min(z1, bz1), cx1, cy1, bz1);
        }
    }

    private void add(int x0, int y0, int z0, int x1, int y1, int z1) {
        if (x1 <= x0 || y1 <= y0 || z1 <= z0) return;
        if (count * 6 == boxes.length) boxes = Arrays.copyOf(boxes, boxes.length * 2);
        int i = count++ * 6;
        boxes[i] = x0;
        boxes[i + 1] = y0;
        boxes[i + 2] = z0;
        boxes[i + 3] = x1;
        boxes[i + 4] = y1;
        boxes[i + 5] = z1;
    }
}
