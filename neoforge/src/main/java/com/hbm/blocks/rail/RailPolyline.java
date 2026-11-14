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

package com.hbm.blocks.rail;

import java.util.List;
import net.minecraft.world.phys.Vec3;

final class RailPolyline {

    static final int X = 0;
    static final int Y = 1;
    static final int Z = 2;

    private final double[] offsets;

    RailPolyline(List<Vec3> nodes, float yaw) {
        offsets = new double[nodes.size() * 3];
        for (int i = 0; i < nodes.size(); i++) {
            Vec3 turned = nodes.get(i).yRot(yaw);
            offsets[i * 3 + X] = turned.x;
            offsets[i * 3 + Y] = turned.y;
            offsets[i * 3 + Z] = turned.z;
        }
    }

    int links() {
        return offsets.length / 3 - 1;
    }

    double node(int node, int axis, double origin) {
        return origin + offsets[node * 3 + axis];
    }

    double project(int link, double originX, double originZ, double px, double pz) {
        double ax = node(link, X, originX);
        double az = node(link, Z, originZ);
        double abX = node(link + 1, X, originX) - ax;
        double abZ = node(link + 1, Z, originZ) - az;
        return ((px - ax) * abX + (pz - az) * abZ) / (abX * abX + abZ * abZ);
    }

    double along(int link, int axis, double origin, double t) {
        double a = node(link, axis, origin);
        double b = node(link + 1, axis, origin);
        if (t < 0) return a;
        if (t > 1) return b;
        return a + (b - a) * t;
    }
}
