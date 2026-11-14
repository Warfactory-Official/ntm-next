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
import com.hbm.blocks.network.ConnectorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ConnectorSuperBlockEntity extends ConnectorBlockEntity {

    public ConnectorSuperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONNECTOR_REDWIRE_SUPER.get(), pos, state);
    }

    @Override
    public Vec3[] wireMounts() {
        Direction dir = getBlockState().getValue(ConnectorBlock.FACING);
        return new Vec3[] {
            new Vec3(
                    0.5 + dir.getStepX() * 0.375,
                    0.5 + dir.getStepY() * 0.375,
                    0.5 + dir.getStepZ() * 0.375)
        };
    }

    @Override
    public double wireReach() {
        return 100;
    }
}
