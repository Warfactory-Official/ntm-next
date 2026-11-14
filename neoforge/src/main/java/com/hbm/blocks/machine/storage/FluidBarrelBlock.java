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

package com.hbm.blocks.machine.storage;

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.machine.storage.FluidBarrelBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.PersistentItemTooltipProvider;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.BlockMenus;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.ModDataComponents;
import com.hbm.items.PersistentInfoBlockItem;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class FluidBarrelBlock extends Block implements PersistentItemTooltipProvider {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    private static final VoxelShape SHAPE = Shapes.box(2 / 16D, 0, 2 / 16D, 14 / 16D, 1, 14 / 16D);

    public final int capacity;
    private final int descLines;

    public FluidBarrelBlock(Properties props, int capacity, int descLines) {
        super(props);
        this.capacity = capacity;
        this.descLines = descLines;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    public static class Functional extends FluidBarrelBlock
            implements TickingEntityBlock, CapabilityBlock {

        public static final BooleanProperty NORTH = FluidBarrelBlock.NORTH;
        public static final BooleanProperty EAST = FluidBarrelBlock.EAST;
        public static final BooleanProperty SOUTH = FluidBarrelBlock.SOUTH;
        public static final BooleanProperty WEST = FluidBarrelBlock.WEST;

        public Functional(Properties props, int capacity, int descLines) {
            super(props, capacity, descLines);
            registerDefaultState(
                    stateDefinition
                            .any()
                            .setValue(NORTH, false)
                            .setValue(EAST, false)
                            .setValue(SOUTH, false)
                            .setValue(WEST, false));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(NORTH, EAST, SOUTH, WEST);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new FluidBarrelBlockEntity(pos, state);
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
            if (stack.getItem() instanceof FluidIdentifierItem && !player.isShiftKeyDown()) {
                if (!level.isClientSide()
                        && level.getBlockEntity(pos) instanceof FluidBarrelBlockEntity barrel) {
                    FluidIdentifierData data =
                            stack.getOrDefault(
                                    ModDataComponents.FLUID_IDENTIFIER.get(),
                                    FluidIdentifierData.EMPTY);
                    Fluid target = data.primary();
                    barrel.tank.setTankTypeByIdentifier(target == Fluids.EMPTY ? null : target);
                    barrel.setChanged();
                    player.sendSystemMessage(
                            Component.translatable("desc.shared.changedTypeTo")
                                    .append(NTMFluidProperties.getDisplayName(target))
                                    .append("!")
                                    .withStyle(ChatFormatting.YELLOW));
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        @Override
        protected InteractionResult useWithoutItem(
                BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player.isSecondaryUseActive()) return InteractionResult.SUCCESS;
            if (!level.isClientSide()
                    && level.getBlockEntity(pos) instanceof MenuProvider provider) {
                BlockMenus.open(player, provider);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        protected void neighborChanged(
                BlockState state,
                Level level,
                BlockPos pos,
                Block block,
                @Nullable Orientation orientation,
                boolean movedByPiston) {
            super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
            if (level.getBlockEntity(pos) instanceof FluidBarrelBlockEntity barrel)
                barrel.refreshConnectors();
        }

        @Override
        protected boolean hasAnalogOutputSignal(BlockState state) {
            return true;
        }

        @Override
        protected int getAnalogOutputSignal(
                BlockState state, Level level, BlockPos pos, Direction direction) {
            return level.getBlockEntity(pos) instanceof MachineBlockEntity machine
                    ? machine.getComparatorPower()
                    : 0;
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(ModBlockEntities.FLUID_BARREL)
                    .fluidIn()
                    .fluidOut()
                    .items()
                    .fluidFaces(
                            FluidBarrelBlockEntity.class,
                            (be, face) -> face.fluid() == null || be.connectsFluid(face.fluid()));
        }

        @java.lang.Override
        protected net.minecraft.world.level.block.state.BlockState rotate(
                net.minecraft.world.level.block.state.BlockState state,
                net.minecraft.world.level.block.Rotation rotation) {
            if (rotation == net.minecraft.world.level.block.Rotation.CLOCKWISE_90)
                return state.setValue(NORTH, state.getValue(WEST))
                        .setValue(EAST, state.getValue(NORTH))
                        .setValue(SOUTH, state.getValue(EAST))
                        .setValue(WEST, state.getValue(SOUTH));
            if (rotation == net.minecraft.world.level.block.Rotation.CLOCKWISE_180)
                return state.setValue(NORTH, state.getValue(SOUTH))
                        .setValue(EAST, state.getValue(WEST))
                        .setValue(SOUTH, state.getValue(NORTH))
                        .setValue(WEST, state.getValue(EAST));
            if (rotation == net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90)
                return state.setValue(NORTH, state.getValue(EAST))
                        .setValue(EAST, state.getValue(SOUTH))
                        .setValue(SOUTH, state.getValue(WEST))
                        .setValue(WEST, state.getValue(NORTH));
            return state;
        }

        @java.lang.Override
        protected net.minecraft.world.level.block.state.BlockState mirror(
                net.minecraft.world.level.block.state.BlockState state,
                net.minecraft.world.level.block.Mirror mirror) {
            if (mirror == net.minecraft.world.level.block.Mirror.LEFT_RIGHT)
                return state.setValue(NORTH, state.getValue(SOUTH))
                        .setValue(SOUTH, state.getValue(NORTH));
            if (mirror == net.minecraft.world.level.block.Mirror.FRONT_BACK)
                return state.setValue(EAST, state.getValue(WEST))
                        .setValue(WEST, state.getValue(EAST));
            return state;
        }
    }

    public static class BarrelBlockItem extends PersistentInfoBlockItem {

        public BarrelBlockItem(FluidBarrelBlock block, Properties props) {
            super(block, props);
        }

        @Override
        public void appendHoverText(
                ItemStack stack,
                Item.TooltipContext context,
                TooltipDisplay display,
                Consumer<Component> adder,
                TooltipFlag flag) {
            FluidBarrelBlock barrel = (FluidBarrelBlock) getBlock();
            String key = "desc.hbm." + BuiltInRegistries.BLOCK.getKey(barrel).getPath();
            adder.accept(
                    Component.translatable(key + ".capacity", String.format("%,d", barrel.capacity))
                            .withStyle(ChatFormatting.AQUA));
            for (int i = 1; i <= barrel.descLines; i++) {
                adder.accept(Component.translatable(key + ".line" + i));
            }
            super.appendHoverText(stack, context, display, adder, flag);
        }
    }

    @Override
    public void appendPersistentInfo(ItemStack stack, Consumer<Component> adder) {
        FluidStackNTM content = stack.get(ModDataComponents.FLUID_CONTENT.get());
        if (content == null) return;
        adder.accept(PersistentItemTooltipProvider.tankLine(content, capacity));
    }
}
