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

package com.hbm.blocks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class BeltTravel {

    private BeltTravel() {}

    public static Vec3 clamp(BlockPos cell, Vec3 item) {
        return new Vec3(
                Mth.clamp(item.x, cell.getX(), cell.getX() + 1D),
                item.y,
                Mth.clamp(item.z, cell.getZ(), cell.getZ() + 1D));
    }

    public static Vec3 centreLine(BlockPos cell, Vec3 clamped, Direction against) {
        double x = against.getStepX() != 0 ? clamped.x : cell.getX() + 0.5;
        double z = against.getStepZ() != 0 ? clamped.z : cell.getZ() + 0.5;
        return new Vec3(x, cell.getY() + 0.25, z);
    }

    public static Vec3 snap(BlockPos cell, Vec3 item, Direction against) {
        return centreLine(cell, clamp(cell, item), against);
    }

    public static Vec3 step(Vec3 from, Vec3 lane, Direction against, double speed) {
        double dx = lane.x - against.getStepX() * speed - from.x;
        double dy = lane.y - against.getStepY() * speed - from.y;
        double dz = lane.z - against.getStepZ() * speed - from.z;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return new Vec3(
                from.x + dx / length * speed,
                from.y + dy / length * speed,
                from.z + dz / length * speed);
    }

    public static Vec3 travel(BlockPos cell, Vec3 item, Direction against, double speed) {
        Vec3 clamped = clamp(cell, item);
        return step(clamped, centreLine(cell, clamped, against), against, speed);
    }
}
