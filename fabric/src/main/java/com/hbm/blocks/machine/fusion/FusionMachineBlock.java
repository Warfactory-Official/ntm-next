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

package com.hbm.blocks.machine.fusion;

import com.hbm.blockentity.machine.fusion.FusionPorts;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public abstract class FusionMachineBlock extends MultiblockCoreBlock implements TickingEntityBlock {
    private volatile FusionPorts.Port[][] localPorts =
            new FusionPorts.Port[Direction.VALUES.length][];

    protected FusionMachineBlock(Properties props) {
        super(props);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public abstract List<FusionPorts.Port> links(BlockPos core, Direction facing);

    public final boolean exposes(
            BlockPos core, Direction facing, FusionPorts.Kind kind, BlockPos cell, Direction side) {
        var geometry = localPorts;
        var ports = geometry[facing.ordinal()];
        if (ports == null) {
            ports = links(BlockPos.ZERO, facing).toArray(FusionPorts.Port[]::new);
            var updated = geometry.clone();
            updated[facing.ordinal()] = ports;
            localPorts = updated;
        }
        var dx = cell.getX() - core.getX();
        var dy = cell.getY() - core.getY();
        var dz = cell.getZ() - core.getZ();
        for (var port : ports) {
            var offset = port.cell();
            if (port.kind() == kind
                    && port.face() == side
                    && offset.getX() == dx
                    && offset.getY() == dy
                    && offset.getZ() == dz) return true;
        }
        return false;
    }
}
