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

import com.hbm.blockentity.machine.fusion.FusionPlasmaForgeBlockEntity;
import com.hbm.blockentity.machine.fusion.FusionPorts;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.BlockMenus;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class FusionPlasmaForgeBlock extends FusionMachineBlock implements CapabilityBlock {
    public FusionPlasmaForgeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        return BlockMenus.openOnUse(level, core, player);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FusionPlasmaForgeBlockEntity(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().powerIn().fluidIn().itemsAtCells().fe();
    }

    @Override
    public List<FusionPorts.Port> links(BlockPos core, Direction facing) {
        return FusionPlasmaForgeBlockEntity.links(core, facing);
    }
}
