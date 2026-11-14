/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blocks.machine.pile;

import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.ItemInsertionHandler;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.inventory.material.Materials;
import com.hbm.items.ModItems;
import com.hbm.items.special.Autogen;
import com.hbm.sound.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class DrilledGraphiteBlock extends Block
        implements ToolInteractionHandler, ItemInsertionHandler {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty SHROUDED = BooleanProperty.create("shrouded");
    public static final BooleanProperty PU239 = BooleanProperty.create("pu239");
    public static final BooleanProperty WITHDRAWN = BooleanProperty.create("withdrawn");

    private static final Logger LOGGER = LogUtils.getLogger();

    protected DrilledGraphiteBlock(BlockBehaviour.Properties properties) {
        super(properties);
        var state =
                stateDefinition.any().setValue(AXIS, Direction.Axis.Y).setValue(SHROUDED, false);
        if (state.hasProperty(PU239)) {
            state = state.setValue(PU239, false);
        }
        if (state.hasProperty(WITHDRAWN)) {
            state = state.setValue(WITHDRAWN, false);
        }
        registerDefaultState(state);
    }

    public static BlockBehaviour.Properties pileProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, SHROUDED);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return RotatedPillarBlock.rotatePillar(state, rotation);
    }

    public static boolean isChannelFace(BlockState state, @Nullable Direction side) {
        return side != null && side.getAxis() == state.getValue(AXIS);
    }

    public static BlockState copyShape(BlockState source, BlockState destination) {
        return destination
                .setValue(AXIS, source.getValue(AXIS))
                .setValue(SHROUDED, source.getValue(SHROUDED));
    }

    public static Item aluminiumShell() {
        return Autogen.require(MaterialShape.SHELL, Materials.ALUMINUM);
    }

    public @Nullable Item getInsertedItem() {
        return null;
    }

    protected @Nullable Item getInsertedItem(BlockState state) {
        return getInsertedItem();
    }

    protected @Nullable BlockState checkInteractions(ItemStack stack) {
        var item = stack.getItem();
        if (item == ModItems.PILE_ROD_URANIUM.get()) {
            return ModBlocks.BLOCK_GRAPHITE_FUEL.get().defaultBlockState();
        }
        if (item == ModItems.PILE_ROD_PU239.get()) {
            return ModBlocks.BLOCK_GRAPHITE_FUEL
                    .get()
                    .defaultBlockState()
                    .setValue(DrilledGraphiteBlock.PU239, true);
        }
        if (item == ModItems.PILE_ROD_PLUTONIUM.get()) {
            return ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get().defaultBlockState();
        }
        if (item == ModItems.PILE_ROD_SOURCE.get()) {
            return ModBlocks.BLOCK_GRAPHITE_SOURCE.get().defaultBlockState();
        }
        if (item == ModItems.PILE_ROD_BORON.get()) {
            return ModBlocks.BLOCK_GRAPHITE_ROD.get().defaultBlockState();
        }
        if (item == ModItems.PILE_ROD_LITHIUM.get()) {
            return ModBlocks.BLOCK_GRAPHITE_LITHIUM.get().defaultBlockState();
        }
        if (item == ModItems.CELL_TRITIUM.get()) {
            return ModBlocks.BLOCK_GRAPHITE_TRITIUM.get().defaultBlockState();
        }
        if (item == ModItems.PILE_ROD_DETECTOR.get()) {
            return ModBlocks.BLOCK_GRAPHITE_DETECTOR.get().defaultBlockState();
        }
        return null;
    }

    protected static void ejectItem(
            Level level, BlockPos pos, Direction direction, ItemStack stack) {
        double x = pos.getX() + 0.5 + direction.getStepX() * 0.75;
        double y = pos.getY() + 0.5 + direction.getStepY() * 0.75;
        double z = pos.getZ() + 0.5 + direction.getStepZ() * 0.75;
        var entity = new ItemEntity(level, x, y, z, stack);
        entity.setDeltaMovement(
                direction.getStepX() * 0.25,
                direction.getStepY() * 0.25,
                direction.getStepZ() * 0.25);
        level.addFreshEntity(entity);
    }

    @Override
    public boolean onToolUse(
            Level level,
            @Nullable Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) {
            return false;
        }
        if (player != null && player.isSpectator()) {
            return false;
        }
        var state = level.getBlockState(pos);
        if (!level.isClientSide() && isChannelFace(state, side)) {
            var item = getInsertedItem(state);
            var emptyState =
                    copyShape(state, ModBlocks.BLOCK_GRAPHITE_DRILLED.get().defaultBlockState());
            level.setBlock(pos, emptyState, Block.UPDATE_ALL);
            ejectItem(level, pos, side, new ItemStack(item));
        }
        return true;
    }

    protected static void reportAssembly(Player player, BlockPos pos) {
        player.sendSystemMessage(
                Component.translatable(
                                "desc.shared.cp1FuelAssembly", pos.getX(), pos.getY(), pos.getZ())
                        .withStyle(ChatFormatting.GOLD));
    }

    protected static void reportFuelProgress(
            Player player, int progress, int maximum, int neutrons) {
        player.sendSystemMessage(
                Component.translatable(
                                "desc.block.graphiteFuel.depletion", progress + "/" + maximum)
                        .withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(
                Component.translatable("desc.shared.flux", neutrons)
                        .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public boolean insertItem(Level level, BlockPos pos, Direction direction, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        var insertedState = checkInteractions(stack);
        if (insertedState == null) {
            return false;
        }
        var initialState = level.getBlockState(pos);
        if (!isChannelFace(initialState, direction)) {
            return false;
        }

        var axis = initialState.getValue(AXIS);
        var cursor = pos.mutable();
        int cellCount = 0;
        boolean ejectLast = false;
        for (int distance = 0; distance <= 3; distance++) {
            var state = level.getBlockState(cursor);
            if (!(state.getBlock() instanceof DrilledGraphiteBlock)) {
                boolean obstructed =
                        state.isCollisionShapeFullBlock(level, cursor)
                                && state.canOcclude()
                                && !state.isSignalSource();
                if (obstructed) {
                    return false;
                }
                ejectLast = true;
                break;
            }
            if (state.getValue(AXIS) != axis) {
                return false;
            }
            if (state.getBlock() instanceof GraphiteDrilledBlock) {
                cellCount++;
                break;
            }
            if (distance == 3) {
                return false;
            }
            cellCount++;
            cursor.move(direction);
        }

        var serverLevel = (ServerLevel) level;
        var registries = serverLevel.registryAccess();
        var emptyData = new CompoundTag();
        BlockState carriedState = insertedState;
        CompoundTag carriedData = emptyData;
        cursor.set(pos);
        for (int cell = 0; cell < cellCount; cell++) {
            var cellPos = cursor.immutable();
            var oldState = serverLevel.getBlockState(cellPos);
            var outgoingEntity = serverLevel.getBlockEntity(cellPos);
            CompoundTag outgoingData = emptyData;
            if (outgoingEntity != null) {
                outgoingData = outgoingEntity.saveCustomOnly(registries);
            }

            var newState = copyShape(oldState, carriedState);
            serverLevel.setBlock(cellPos, newState, Block.UPDATE_KNOWN_SHAPE);
            var incomingEntity = serverLevel.getBlockEntity(cellPos);
            boolean customDataChanged =
                    oldState.getBlock() != newState.getBlock() || !carriedData.equals(outgoingData);
            if (incomingEntity != null && customDataChanged) {
                try (var reporter =
                        new ProblemReporter.ScopedCollector(incomingEntity.problemPath(), LOGGER)) {
                    var input = TagValueInput.create(reporter, registries, carriedData);
                    incomingEntity.loadCustomOnly(input);
                }
                serverLevel.blockEntityChanged(cellPos);
            }
            GraphiteCellUpdates.publish(serverLevel, cellPos, oldState, newState);

            carriedState = oldState;
            carriedData = outgoingData;
            if (cell + 1 < cellCount) {
                cursor.move(direction);
            }
        }

        if (ejectLast) {
            var item =
                    ((DrilledGraphiteBlock) carriedState.getBlock()).getInsertedItem(carriedState);
            ejectItem(serverLevel, cursor, direction, new ItemStack(item));
            serverLevel.playSound(
                    null,
                    cursor.relative(direction),
                    ModSounds.UPGRADE_PLUG.get(),
                    SoundSource.BLOCKS,
                    1.25F,
                    1.0F);
        }
        return true;
    }
}
