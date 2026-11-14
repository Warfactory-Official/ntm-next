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

import com.hbm.api.fluidmk2.FluidCaps;
import com.hbm.blockentity.network.FluidPumpBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.inventory.BlockMenus;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.platform.Caps;
import com.hbm.registration.RegistryHandle;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class FluidPumpBlock extends Block
        implements TickingEntityBlock, LookOverlayProvider, CapabilityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public FluidPumpBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
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
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPumpBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (stack.getItem() instanceof FluidIdentifierItem) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (level.getBlockEntity(pos) instanceof FluidPumpBlockEntity pump) {
                FluidIdentifierData data =
                        stack.getOrDefault(
                                ModDataComponents.FLUID_IDENTIFIER.get(),
                                FluidIdentifierData.EMPTY);
                Fluid type = data.primary();
                if (type != null && type != Fluids.EMPTY) {
                    if (pump.setFluidType(type)) pump.markChanged();
                    player.sendSystemMessage(
                            Component.translatable(
                                    "hbm.message.pumpSet",
                                    NTMFluidProperties.getDisplayName(type)));
                }
            }
            return InteractionResult.SUCCESS;
        }
        return useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof FluidPumpBlockEntity pump) {
            BlockMenus.open(player, pump);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        if (!(level.getBlockEntity(pos) instanceof FluidPumpBlockEntity pump)) return;
        String typeName =
                pump.type == null
                        ? Component.translatable("hbmfluid.none").getString()
                        : NTMFluidProperties.clientName(pump.type);
        info.title(getName())
                .row(
                        LookOverlay.inflow(
                                Component.translatable(
                                                "overlay.hbm.pump_rate",
                                                typeName,
                                                pump.pressure,
                                                String.format(Locale.ROOT, "%,d", pump.rate))
                                        .append(
                                                Component.literal(" ->")
                                                        .withStyle(ChatFormatting.RED))))
                .row(
                        Component.translatable(
                                "overlay.hbm.priority",
                                Component.literal(pump.priority.name())
                                        .withStyle(ChatFormatting.YELLOW)));
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().fluidIn().fluidOut().selfProvided();
    }

    @Override
    public void declareExtraCaps(RegistryHandle<? extends Block> self) {
        Caps.pending.add(
                new Caps.Registration<>(
                        FluidCaps.PROVIDER,
                        (Supplier<? extends BlockEntityType<? extends FluidPumpBlockEntity>>)
                                ModBlockEntities.PIPE_PUMP,
                        (be, face) ->
                                face.side() == be.output() && be.carries(face.fluid())
                                        ? be
                                        : null));
        Caps.pending.add(
                new Caps.Registration<>(
                        FluidCaps.RECEIVER,
                        (Supplier<? extends BlockEntityType<? extends FluidPumpBlockEntity>>)
                                ModBlockEntities.PIPE_PUMP,
                        (be, face) ->
                                face.side() == be.intake() && be.carries(face.fluid())
                                        ? be
                                        : null));
    }

    @java.lang.Override
    protected void neighborChanged(
            net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.Block neighborBlock,
            net.minecraft.world.level.redstone.Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        net.minecraft.world.level.block.entity.BlockEntity hbm$be = level.getBlockEntity(pos);
        if (hbm$be instanceof FluidPumpBlockEntity)
            ((FluidPumpBlockEntity) hbm$be).refreshRedstone();
    }

    @java.lang.Override
    protected void onPlace(
            net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        net.minecraft.world.level.block.entity.BlockEntity hbm$be = level.getBlockEntity(pos);
        if (hbm$be instanceof FluidPumpBlockEntity)
            ((FluidPumpBlockEntity) hbm$be).refreshRedstone();
    }
}
