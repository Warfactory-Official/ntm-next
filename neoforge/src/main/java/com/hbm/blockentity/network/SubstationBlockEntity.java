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

public final class SubstationBlockEntity extends PylonBaseBlockEntity {

    private static final Vec3[] X_MOUNTS = mountsAlong(new Vec3(1, 0, 0));
    private static final Vec3[] Z_MOUNTS =
            mountsAlong(new Vec3(1, 0, 0).yRot((float) (Math.PI / 2)));

    public SubstationBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.SUBSTATION.get(), position, state);
    }

    private static Vec3[] mountsAlong(Vec3 arm) {
        double[] offsets = {0.5, 1.5, -0.5, -1.5};
        var mounts = new Vec3[offsets.length];
        for (int index = 0; index < offsets.length; index++) {
            double offset = offsets[index];
            mounts[index] = new Vec3(0.5 + offset * arm.x, 5.25, 0.5 + offset * arm.z);
        }
        return mounts;
    }

    @Override
    public int wireFamily() {
        return FOUR_WIRE;
    }

    @Override
    public Vec3[] wireMounts() {
        var facing = getBlockState().getValue(MultiblockCoreBlock.FACING);
        if (facing.getAxis() == Direction.Axis.X) {
            return Z_MOUNTS;
        }
        return X_MOUNTS;
    }

    @Override
    public double wireReach() {
        return 20;
    }

    @Override
    protected Vec3 wireCenter() {
        var position = getBlockPos();
        return new Vec3(position.getX() + 0.5, position.getY() + 5.25, position.getZ() + 0.5);
    }
}
