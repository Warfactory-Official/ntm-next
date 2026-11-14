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

import com.hbm.blockentity.machine.dfc.DfcEmitterBlockEntity;
import com.hbm.blockentity.machine.dfc.DfcInjectorBlockEntity;
import com.hbm.blockentity.machine.dfc.DfcReceiverBlockEntity;
import com.hbm.blockentity.machine.dfc.DfcStabilizerBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.BlockMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class CoreComponentBlock extends Block implements TickingEntityBlock, CapabilityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    private final Kind kind;

    public CoreComponentBlock(Kind kind, BlockBehaviour.Properties props) {
        super(props);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof MenuProvider provider))
            return InteractionResult.PASS;
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level.isClientSide()) BlockMenus.open(player, provider);
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case EMITTER -> new DfcEmitterBlockEntity(pos, state);
            case RECEIVER -> new DfcReceiverBlockEntity(pos, state);
            case INJECTOR -> new DfcInjectorBlockEntity(pos, state);
            case STABILIZER -> new DfcStabilizerBlockEntity(pos, state);
        };
    }

    @Override
    public MachineCaps caps() {
        return switch (kind) {
            case EMITTER -> MachineCaps.of(ModBlockEntities.CORE_EMITTER).powerIn().fluidIn().fe();
            case RECEIVER ->
                    MachineCaps.of(ModBlockEntities.CORE_RECEIVER).powerOut().fluidIn().fe();
            case INJECTOR -> MachineCaps.of(ModBlockEntities.CORE_INJECTOR).fluidIn().items();
            case STABILIZER ->
                    MachineCaps.of(ModBlockEntities.CORE_STABILIZER).powerIn().items().fe();
        };
    }

    public enum Kind {
        EMITTER,
        RECEIVER,
        INJECTOR,
        STABILIZER
    }
}
