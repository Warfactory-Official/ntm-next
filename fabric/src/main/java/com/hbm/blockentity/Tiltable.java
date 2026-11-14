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

package com.hbm.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

public interface Tiltable {

    BooleanProperty TILT = BooleanProperty.create("tilted");

    enum Policy {
        UNAVOIDABLE,
        CONFIGURED
    }

    BlockPos getBlockPos();

    int getSupportCheckCount();

    @Nullable BlockPos getSupportCheckPosition(int index);

    boolean isTilted();

    void tickTilt(Policy policy, boolean extraHeavy);

    default BlockPos floorGrid3(int index) {
        return floorGrid(1, 2, index);
    }

    default BlockPos floorGrid5(int index) {
        return floorGrid(2, 3, index);
    }

    default BlockPos floorGrid7(int index) {
        return floorGrid(3, 4, index);
    }

    private BlockPos floorGrid(int radius, int rows, int index) {
        BlockPos pos = getBlockPos();
        return new BlockPos(
                pos.getX() - radius + 2 * (index / rows),
                pos.getY() - 1,
                pos.getZ() - radius + 2 * (index % rows));
    }
}
