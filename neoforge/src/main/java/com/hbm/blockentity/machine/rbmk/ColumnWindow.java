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

package com.hbm.blockentity.machine.rbmk;

import com.hbm.util.ChunkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class ColumnWindow {

    private final int side;
    private int targetX;
    private int targetY;
    private int targetZ;
    private int rotation;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    public ColumnWindow(int side) {
        this.side = side;
    }

    public int targetX() {
        return targetX;
    }

    public int targetY() {
        return targetY;
    }

    public int targetZ() {
        return targetZ;
    }

    public int rotation() {
        return rotation;
    }

    public void link(int x, int y, int z) {
        targetX = x;
        targetY = y;
        targetZ = z;
    }

    public void rotate() {
        rotation = (rotation + 1) & 3;
    }

    public int offsetX(int index) {
        int i = index % side - side / 2, j = index / side - side / 2;
        return switch (rotation) {
            case 1 -> -j;
            case 2 -> -i;
            case 3 -> j;
            default -> i;
        };
    }

    public int offsetZ(int index) {
        int i = index % side - side / 2, j = index / side - side / 2;
        return switch (rotation) {
            case 1 -> i;
            case 2 -> -j;
            case 3 -> -i;
            default -> j;
        };
    }

    public @Nullable RBMKBaseBlockEntity column(Level level, int index) {
        cursor.set(targetX + offsetX(index), targetY, targetZ + offsetZ(index));
        return ChunkUtil.blockEntityIfLoaded(level, cursor) instanceof RBMKBaseBlockEntity column
                ? column
                : null;
    }

    public void load(ValueInput input) {
        targetX = input.getIntOr("tX", 0);
        targetY = input.getIntOr("tY", 0);
        targetZ = input.getIntOr("tZ", 0);
        rotation = input.getIntOr("rotation", 0);
        if (rotation < 0 || rotation > 3)
            throw new IllegalArgumentException("Invalid panel rotation " + rotation);
    }

    public void save(ValueOutput output) {
        output.putInt("tX", targetX);
        output.putInt("tY", targetY);
        output.putInt("tZ", targetZ);
        output.putInt("rotation", rotation);
    }
}
