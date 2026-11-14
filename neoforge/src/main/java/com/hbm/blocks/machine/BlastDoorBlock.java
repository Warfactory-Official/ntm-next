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

package com.hbm.blocks.machine;

import com.hbm.blockentity.door.BlastDoorBlockEntity;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.generic.GenericDoorBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.interfaces.TriggerHandler;
import com.hbm.interfaces.TriggerResult;
import com.hbm.items.ModItems;
import com.hbm.items.tool.LockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class BlastDoorBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, TriggerHandler {
    public static final int LEAF_TOP = 5;

    public BlastDoorBlock(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(GenericDoorBlock.OPEN, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(GenericDoorBlock.OPEN);
    }

    @Override
    public boolean cellsOpen() {
        return true;
    }

    @Override
    public boolean cellsSealRadiation() {
        return true;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlastDoorBlockEntity(pos, state);
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
        if (held.getItem() instanceof LockItem || held.is(ModItems.KEY_KIT.get()))
            return InteractionResult.PASS;
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof BlastDoorBlockEntity door)
            door.requestToggle(player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public TriggerResult trigger(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide() || detonator != null && detonator.isSpectator())
            return TriggerResult.NONE;
        if (!(level.getBlockEntity(pos) instanceof BlastDoorBlockEntity door))
            return TriggerResult.NONE;
        if (door.isLocked()) return TriggerResult.INCOMPATIBLE;
        door.requestToggle(null);
        return TriggerResult.ACTUATED;
    }

    @Override
    public boolean wantsNeighborUpdates() {
        return true;
    }

    @Override
    public void cellNeighborChanged(ServerLevel level, BlockPos core, BlockPos cell) {
        if ((cell.equals(core) || cell.equals(core.above(6)))
                && level.getBlockEntity(core) instanceof BlastDoorBlockEntity door)
            door.refreshSignals();
    }

    @Override
    public void hbm$blockEntityLoaded(ServerLevel level, BlockEntity be) {
        super.hbm$blockEntityLoaded(level, be);
        ((BlastDoorBlockEntity) be).refreshSignals();
    }
}
