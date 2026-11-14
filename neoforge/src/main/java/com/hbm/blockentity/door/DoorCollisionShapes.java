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

package com.hbm.blockentity.door;

import java.util.Arrays;
import net.minecraft.world.phys.AABB;

public final class DoorCollisionShapes {

    public static final DoorCollisionShapes DEFAULT = new DoorCollisionShapes(new Rule[0]);

    private final Rule[] rules;

    private DoorCollisionShapes(Rule[] rules) {
        this.rules = rules;
    }

    public DoorCollisionShapes closed(Box box) {
        return with(false, Cell.ANY, box);
    }

    public DoorCollisionShapes closed(Cell where, Box box) {
        return with(false, where, box);
    }

    public DoorCollisionShapes open(Box box) {
        return with(true, Cell.ANY, box);
    }

    public DoorCollisionShapes open(Cell where, Box box) {
        return with(true, where, box);
    }

    private DoorCollisionShapes with(boolean open, Cell where, Box box) {
        var next = Arrays.copyOf(rules, rules.length + 1);
        next[rules.length] = new Rule(open, where, box);
        return new DoorCollisionShapes(next);
    }

    public AABB bound(int x, int y, int z, boolean open, boolean forCollision) {
        for (var rule : rules) {
            if (rule.open == open && rule.where.test(x, y, z)) {
                return forCollision ? rule.box.collision() : rule.box.selection();
            }
        }
        return open ? Box.ZERO.selection : Box.FULL.selection;
    }

    public record Cell(int axis, int value, boolean above) {

        static final Cell ANY = new Cell(-1, 0, false);

        public static Cell x(int value) {
            return new Cell(0, value, false);
        }

        public static Cell y(int value) {
            return new Cell(1, value, false);
        }

        public static Cell z(int value) {
            return new Cell(2, value, false);
        }

        public static Cell yAbove(int value) {
            return new Cell(1, value, true);
        }

        boolean test(int x, int y, int z) {
            if (axis < 0) return true;
            int at = axis == 0 ? x : axis == 1 ? y : z;
            return above ? at > value : at == value;
        }
    }

    public record Box(AABB selection, AABB collision) {

        public static final Box FULL = box(0, 0, 0, 1, 1, 1);
        public static final Box ZERO = box(0, 0, 0, 0, 0, 0);

        public static Box box(
                double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            var box = aabb(minX, minY, minZ, maxX, maxY, maxZ);
            return new Box(box, box);
        }

        public static Box floor(double minX, double minZ, double maxX, double maxZ, double height) {
            return new Box(
                    aabb(minX, 0, minZ, maxX, height, maxZ), aabb(minX, 0, minZ, maxX, 0, maxZ));
        }

        public static Box split(Box selection, Box collision) {
            return new Box(selection.selection, collision.collision);
        }

        private static AABB aabb(
                double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            if (!(0 <= minX
                    && minX <= maxX
                    && maxX <= 1
                    && 0 <= minY
                    && minY <= maxY
                    && maxY <= 1
                    && 0 <= minZ
                    && minZ <= maxZ
                    && maxZ <= 1)) {
                throw new IllegalArgumentException(
                        "door cell box "
                                + minX
                                + ","
                                + minY
                                + ","
                                + minZ
                                + " to "
                                + maxX
                                + ","
                                + maxY
                                + ","
                                + maxZ
                                + " leaves the unit cube or turns inside out");
            }
            return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    private record Rule(boolean open, Cell where, Box box) {}
}
