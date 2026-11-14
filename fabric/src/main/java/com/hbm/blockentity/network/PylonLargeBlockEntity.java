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

package com.hbm.blockentity.network;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PylonLargeBlockEntity extends PylonBaseBlockEntity {

    private static final double CROSSARM_HEIGHT = 11.5D;
    private static final double ARM_LIFT = 0.75D + 0.0625D;
    private static final double ARM_REACH = 3.375D;

    public PylonLargeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PYLON_LARGE.get(), pos, state);
    }

    private static float armYaw(Direction facing) {
        return (float)
                (Math.PI
                        * switch (facing) {
                            case NORTH -> 0D;
                            case WEST -> 0.25D;
                            case SOUTH -> 0.5D;
                            default -> 0.75D;
                        });
    }

    @Override
    public int wireFamily() {
        return FOUR_WIRE;
    }

    @Override
    public Vec3[] wireMounts() {
        Vec3 arm =
                new Vec3(ARM_REACH, 0, 0)
                        .yRot(armYaw(getBlockState().getValue(MultiblockCoreBlock.FACING)));
        return new Vec3[] {
            new Vec3(0.5 + arm.x, CROSSARM_HEIGHT + ARM_LIFT, 0.5 + arm.z),
            new Vec3(0.5 + arm.x, CROSSARM_HEIGHT - ARM_LIFT, 0.5 + arm.z),
            new Vec3(0.5 - arm.x, CROSSARM_HEIGHT + ARM_LIFT, 0.5 - arm.z),
            new Vec3(0.5 - arm.x, CROSSARM_HEIGHT - ARM_LIFT, 0.5 - arm.z),
        };
    }

    @Override
    public double wireReach() {
        return 100;
    }
}
