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

import com.hbm.blockentity.ordnance.launch.LaunchTableBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.TriggerHandler;
import com.hbm.interfaces.TriggerResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class LaunchTableBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, TriggerHandler, CapabilityBlock {
    private static final int SCAFFOLD_OFFSET = 3;
    private static final int SCAFFOLD_HEIGHT = 12;

    public LaunchTableBlock(Properties props) {
        super(props);
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;

        BlockState core = level.getBlockState(pos);
        if (core.getBlock() != this) return;
        BlockPos foot = pos.relative(core.getValue(FACING).getClockWise(), SCAFFOLD_OFFSET);
        for (int i = 1; i < SCAFFOLD_HEIGHT; i++) level.removeBlock(foot.above(i), false);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaunchTableBlockEntity(pos, state);
    }

    @Override
    public TriggerResult trigger(Level level, BlockPos pos, Entity detonator) {
        if (level.isClientSide()) return TriggerResult.NONE;
        if (detonator != null && detonator.isSpectator()) return TriggerResult.NONE;
        FoldedOwner owner = ownerOf(level, pos);
        BlockPos core = owner != null ? owner.pos() : pos;
        if (level.getBlockEntity(core) instanceof LaunchTableBlockEntity table) {
            if (!table.launchReady()) return TriggerResult.MISSING_COMPONENT;
            return table.launchDesignated();
        }
        return TriggerResult.NONE;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.LARGE_LAUNCH_TABLE)
                .powerIn()
                .fluidIn()
                .items()
                .fe()
                .faces(LaunchTableBlockEntity.class, (be, side) -> side.getAxis().isHorizontal())
                .fluidFaces(
                        LaunchTableBlockEntity.class,
                        (be, face) -> face.side().getAxis().isHorizontal());
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
        if (hbm$be instanceof LaunchTableBlockEntity)
            ((LaunchTableBlockEntity) hbm$be).sampleSignal();
    }
}
