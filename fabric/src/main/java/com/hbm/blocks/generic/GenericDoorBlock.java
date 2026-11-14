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

package com.hbm.blocks.generic;

import com.hbm.blockentity.door.DoorBlockEntity;
import com.hbm.blockentity.door.DoorType;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.config.HbmConfig;
import com.hbm.config.RuntimeConfig;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import com.hbm.interfaces.TriggerHandler;
import com.hbm.interfaces.TriggerResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class GenericDoorBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, TriggerHandler, ToolInteractionHandler {
    public static final IntegerProperty SKIN = IntegerProperty.create("skin", 0, 6);
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    private static @Nullable IntegerProperty constructionSkin;

    public final DoorType kind;
    private final @Nullable IntegerProperty skin;
    private final boolean sealsRadiation;

    public GenericDoorBlock(BlockBehaviour.Properties properties, DoorType kind) {
        this(properties, kind, false);
    }

    public GenericDoorBlock(
            BlockBehaviour.Properties properties, DoorType kind, boolean sealsRadiation) {
        super(constructionProperties(properties, kind));
        this.kind = kind;
        this.sealsRadiation = sealsRadiation;
        skin = constructionSkin;
        constructionSkin = null;

        var initialState = defaultBlockState().setValue(OPEN, false);
        if (skin != null) {
            initialState = initialState.setValue(skin, 0);
        }
        registerDefaultState(initialState);
    }

    private static BlockBehaviour.Properties constructionProperties(
            BlockBehaviour.Properties properties, DoorType kind) {
        int count = kind.getSkinCount();
        if (count <= 1) {
            constructionSkin = null;
        } else if (count == 7) {
            constructionSkin = SKIN;
        } else {
            constructionSkin = IntegerProperty.create("skin", 0, count - 1);
        }
        return properties;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPEN);
        if (constructionSkin != null) {
            builder.add(constructionSkin);
        }
    }

    public @Nullable IntegerProperty skinProperty() {
        return skin;
    }

    public static int skinIndex(BlockState state) {
        var door = (GenericDoorBlock) state.getBlock();
        return door.skin == null ? 0 : state.getValue(door.skin);
    }

    public BlockState withSkin(BlockState state, int index) {
        return skin == null ? state : state.setValue(skin, index);
    }

    @Override
    public boolean cellsSealRadiation() {
        return sealsRadiation;
    }

    @Override
    public boolean cellsOpen() {
        return true;
    }

    private static @Nullable DoorBlockEntity doorAt(BlockGetter level, BlockPos core) {
        return level.getBlockEntity(core) instanceof DoorBlockEntity door ? door : null;
    }

    private VoxelShape doorShape(
            BlockState state, BlockGetter level, BlockPos pos, boolean forCollision) {
        var door = doorAt(level, pos);
        boolean open = door != null && door.phase != DoorBlockEntity.CLOSED;
        return coreShape(state.getValue(FACING), open, forCollision);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return doorShape(state, level, pos, false);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return doorShape(state, level, pos, true);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DoorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (((RuntimeConfig) HbmConfig.RUNTIME).ultraLarpMode() && !kind.isLarpExempt()) {
            return InteractionResult.SUCCESS;
        }
        var door = doorAt(level, pos);
        return door != null && door.requestToggle(player)
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
    }

    @Override
    public boolean onToolUse(
            Level level,
            @Nullable Player player,
            BlockPos pos,
            @Nullable Direction face,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER
                || player == null
                || player.isSpectator()
                || !player.isShiftKeyDown()
                || !kind.hasSkins()) {
            return false;
        }
        var door = doorAt(level, pos);
        if (door == null) {
            return false;
        }
        if (!level.isClientSide()) {
            door.cycleSkin();
        }
        return true;
    }

    @Override
    public TriggerResult trigger(Level level, BlockPos pos, @Nullable Entity initiator) {
        if (level.isClientSide()) {
            return TriggerResult.NONE;
        }
        if (!kind.isRemote() || initiator != null && initiator.isSpectator()) {
            return TriggerResult.INCOMPATIBLE;
        }
        var door = doorAt(level, pos);
        return door != null && door.requestToggle(null)
                ? TriggerResult.ACTUATED
                : TriggerResult.INCOMPATIBLE;
    }

    @Override
    public boolean wantsNeighborUpdates() {
        return true;
    }

    @Override
    public void cellNeighborChanged(ServerLevel level, BlockPos core, BlockPos cell) {
        var door = doorAt(level, core);
        if (door != null) {
            door.onSignalChanged(cell);
        }
    }
}
