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

package com.hbm.blocks.bomb;

import com.hbm.blockentity.ordnance.launch.MissileErectorBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.TriggerHandler;
import com.hbm.interfaces.TriggerResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class LargeLaunchPadBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, TriggerHandler, CapabilityBlock {

    public LargeLaunchPadBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MissileErectorBlockEntity(pos, state);
    }

    @Override
    public TriggerResult trigger(Level level, BlockPos pos, @Nullable Entity initiator) {
        if (level.isClientSide() || initiator != null && initiator.isSpectator()) {
            return TriggerResult.NONE;
        }
        var owner = ownerOf(level, pos);
        var core = owner == null ? pos : owner.pos();
        if (level.getBlockEntity(core) instanceof MissileErectorBlockEntity launcher) {
            return launcher.launchFromTarget();
        }
        return TriggerResult.NONE;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.LAUNCHPAD_LARGE)
                .powerIn()
                .fluidIn()
                .itemsAtCells()
                .fe()
                .faces(
                        MissileErectorBlockEntity.class,
                        (_, side) -> side.getAxis().isHorizontal())
                .fluidFaces(
                        MissileErectorBlockEntity.class,
                        (_, face) -> {
                            var side = face.side();
                            return side == null || side.getAxis().isHorizontal();
                        });
    }

    @java.lang.Override
    public boolean wantsNeighborUpdates() {
        return true;
    }

    @java.lang.Override
    public void cellNeighborChanged(
            net.minecraft.server.level.ServerLevel level,
            net.minecraft.core.BlockPos core,
            net.minecraft.core.BlockPos cell) {
        net.minecraft.world.level.block.entity.BlockEntity hbm$be = level.getBlockEntity(core);
        if (hbm$be instanceof MissileErectorBlockEntity)
            ((MissileErectorBlockEntity) hbm$be).refreshLaunchSignal();
    }
}
