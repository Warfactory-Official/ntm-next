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

package com.hbm.blocks.machine.rbmk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class MiniPanelHit {

    private MiniPanelHit() {}

    static int columnOf(BlockState state, BlockHitResult hit) {
        if (hit.getDirection().getAxis() == Direction.Axis.Y) return -1;

        BlockPos pos = hit.getBlockPos();
        Vec3 at = hit.getLocation();
        double hitX = at.x - pos.getX();
        double hitZ = at.z - pos.getZ();
        if (hitX == 0 || hitX == 1 || hitZ == 0 || hitZ == 1) return -1;

        return switch (state.getValue(RBMKMiniPanelBlock.FACING)) {
            case NORTH -> hitX < 0.5 ? 1 : 0;
            case SOUTH -> hitX > 0.5 ? 1 : 0;
            case WEST -> hitZ > 0.5 ? 1 : 0;
            case EAST -> hitZ < 0.5 ? 1 : 0;
            default -> 0;
        };
    }

    static int rowOffset(BlockHitResult hit) {
        return hit.getLocation().y - hit.getBlockPos().getY() < 0.5 ? 2 : 0;
    }
}
