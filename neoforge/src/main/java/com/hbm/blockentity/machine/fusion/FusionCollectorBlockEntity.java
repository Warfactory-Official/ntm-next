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

package com.hbm.blockentity.machine.fusion;

import com.hbm.blocks.ModBlockEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FusionCollectorBlockEntity extends BlockEntity {

    public FusionCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_COLLECTOR.get(), pos, state);
    }

    public static List<FusionPorts.Port> links(BlockPos core, Direction facing) {
        Direction dir = facing.getOpposite();
        return List.of(
                new FusionPorts.Port(
                        FusionPorts.Kind.PLASMA,
                        core.offset(dir.getStepX() * 2, 2, dir.getStepZ() * 2),
                        dir));
    }
}
