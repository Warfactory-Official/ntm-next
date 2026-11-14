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

import com.hbm.api.conveyor.ConveyorBelt;
import com.hbm.api.conveyor.ConveyorInputHandler;
import com.hbm.api.conveyor.ConveyorItem;
import com.hbm.blockentity.network.CraneSplitterBlockEntity;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.entity.item.MovingItemEntity;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CraneSplitterBlock extends MultiblockCoreBlock
        implements TickingEntityBlock,
                ConveyorBelt,
                ConveyorInputHandler,
                ToolInteractionHandler,
                LookOverlayProvider {
    public CraneSplitterBlock(Properties properties) {
        super(properties);
    }

    public static Direction rightLane(Direction facing) {
        return facing.getCounterClockWise();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CraneSplitterBlockEntity(pos, state);
    }

    private static @Nullable BlockPos coreOf(Level level, BlockPos pos) {
        var owner = MultiblockCoreBlock.ownerOf(level, pos);
        if (owner != null) {
            return owner.pos();
        }
        if (level.getBlockEntity(pos) instanceof CraneSplitterBlockEntity) {
            return pos;
        }
        return null;
    }

    private static @Nullable CraneSplitterBlockEntity splitterOf(Level level, BlockPos pos) {
        var core = coreOf(level, pos);
        if (core == null) {
            return null;
        }
        if (level.getBlockEntity(core) instanceof CraneSplitterBlockEntity splitter) {
            return splitter;
        }
        return null;
    }

    public Direction getTravelDirection(Level level, BlockPos pos, @Nullable Vec3 itemPos) {
        var core = coreOf(level, pos);
        if (core == null) {
            return Direction.NORTH;
        }
        return level.getBlockState(core).getValue(FACING);
    }

    @Override
    public boolean canItemEnter(
            Level level, BlockPos pos, @Nullable Direction direction, ConveyorItem rider) {
        return direction == getTravelDirection(level, pos, null);
    }

    @Override
    public void onItemEnter(
            Level level, BlockPos pos, @Nullable Direction direction, ConveyorItem rider) {
        var splitter = splitterOf(level, pos);
        if (splitter == null) {
            return;
        }

        var shares = splitter.splitStack(rider.getItemStack());
        var core = splitter.getBlockPos();
        var facing = level.getBlockState(core).getValue(FACING);
        var right = core.relative(rightLane(facing));
        emitShare(level, core, shares[0]);
        emitShare(level, right, shares[1]);
    }

    private static void emitShare(Level level, BlockPos lane, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        var rider = new MovingItemEntity(level);
        rider.setItemStack(stack);
        rider.setPos(lane.getX() + 0.5, lane.getY() + 0.25, lane.getZ() + 0.5);
        level.addFreshEntity(rider);
    }

    @Override
    public boolean canItemStay(Level level, BlockPos pos, Vec3 itemPos) {
        return true;
    }

    @Override
    public Vec3 getTravelPosition(Level level, BlockPos pos, Vec3 itemPos, double speed) {
        var against = getTravelDirection(level, pos, itemPos);
        return BeltTravel.travel(pos, itemPos, against, speed);
    }

    @Override
    public Vec3 getSnappingPosition(Level level, BlockPos pos, Vec3 itemPos) {
        var against = getTravelDirection(level, pos, itemPos);
        return BeltTravel.snap(pos, itemPos, against);
    }

    @Override
    public boolean onToolUse(
            Level level,
            @Nullable Player player,
            BlockPos pos,
            @Nullable Direction face,
            Vec3 hit,
            ToolType tool) {
        return adjustLane(level, player, pos, pos, tool);
    }

    @Override
    public boolean onToolUse(
            Level level,
            @Nullable Player player,
            BlockPos ownerPos,
            BlockPos clickedPos,
            @Nullable Direction face,
            Vec3 hit,
            ToolType tool) {
        return adjustLane(level, player, ownerPos, clickedPos, tool);
    }

    private static boolean adjustLane(
            Level level,
            @Nullable Player player,
            BlockPos anchorPos,
            BlockPos clickedPos,
            ToolType tool) {
        if (player != null && player.isSpectator()) {
            return false;
        }
        if (level.isClientSide()) {
            return true;
        }
        if (tool != ToolType.SCREWDRIVER) {
            return false;
        }

        var splitter = splitterOf(level, anchorPos);
        if (splitter == null) {
            return false;
        }

        int adjustment = 1;
        if (player != null && player.isShiftKeyDown()) {
            adjustment = -1;
        }
        boolean left = clickedPos.equals(splitter.getBlockPos());
        splitter.adjustRatio(left, adjustment);
        return true;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay card) {
        var splitter = splitterOf(level, pos);
        if (splitter == null) {
            return;
        }

        card.title(getName());
        card.row(
                Component.translatable(
                        "overlay.hbm.splitter_ratio", splitter.leftRatio, splitter.rightRatio));
    }
}
