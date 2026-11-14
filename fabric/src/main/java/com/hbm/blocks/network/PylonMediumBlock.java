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

package com.hbm.blocks.network;

import com.hbm.blockentity.network.PylonMediumBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.PhysicalConnector;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class PylonMediumBlock extends MultiblockCoreBlock
        implements EntityBlock, PhysicalConnector {
    private final boolean transformer;
    private final MapCodec<PylonMediumBlock> ownCodec;

    public PylonMediumBlock(boolean transformer, Properties props) {
        super(props);
        this.transformer = transformer;
        this.ownCodec = simpleCodec(p -> new PylonMediumBlock(transformer, p));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return ownCodec;
    }

    private int nodeMask(BlockState state) {
        return transformer ? 1 << state.getValue(FACING).getOpposite().ordinal() : 0;
    }

    @Override
    public boolean hasPowerContact(Level level, BlockPos pos, BlockState state, Direction side) {
        return (nodeMask(state) & (1 << side.ordinal())) != 0;
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this)) {
            CableConductorBlock.mintNode(server, pos, state, nodeMask(state));
        }
    }

    @Override
    protected void removedAt(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.removedAt(state, level, pos, movedByPiston);
        CableConductorBlock.dropNode(level, pos);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOnAtCore(
            ItemStack held,
            BlockState coreState,
            Level level,
            BlockPos core,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        return BasePylonBlock.dyeAt(level, core, held, player);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PylonMediumBlockEntity(pos, state);
    }
}
