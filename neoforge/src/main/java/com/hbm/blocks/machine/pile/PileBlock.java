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

package com.hbm.blocks.machine.pile;

import com.hbm.blockentity.machine.pile.PileCoreBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import com.hbm.util.ChunkUtil;
import com.hbm.world.bulk.BulkAction;
import com.hbm.world.bulk.BulkTransform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PileBlock extends Block
        implements TickingEntityBlock,
                ToolInteractionHandler,
                LookOverlayProvider,
                AssembledMembers.Member {

    public static final EnumProperty<Role> ROLE = EnumProperty.create("role", Role.class);

    public PileBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(ROLE, Role.DUMMY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROLE);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(ROLE) == Role.CORE ? new PileCoreBlockEntity(pos, state) : null;
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (BulkAction.active()) return;
        boolean core = state.getValue(ROLE) == Role.CORE;
        BlockPos owner = core ? null : AssembledMembers.owner(level, pos);
        if (!core) AssembledMembers.release(level, pos);
        if (PileCoreBlockEntity.meltingDown) return;
        if (!core && owner == null) return;
        if (level.getBlockState(pos).is(ModBlocks.PILE_BRICK.get())) return;
        level.setBlockAndUpdate(pos, ModBlocks.PILE_BRICK.get().defaultBlockState());
        PileCoreBlockEntity pile =
                owner == null
                        ? null
                        : ChunkUtil.blockEntityIfLoaded(PileCoreBlockEntity.class, level, owner);
        if (pile != null && !pile.isRemoved()) pile.destroy();
    }

    @Override
    public void verify(ServerLevel level, BlockPos pos, BlockState state, BlockPos owner) {
        if (PileCoreBlockEntity.meltingDown) return;
        if (level.getBlockEntity(owner) instanceof PileCoreBlockEntity) return;
        level.setBlockAndUpdate(pos, ModBlocks.PILE_BRICK.get().defaultBlockState());
    }

    @Override
    public boolean onToolUse(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {

        if (tool == ToolType.HAND_DRILL) {

            if (level.getBlockState(pos).getValue(ROLE) == Role.CORE) {
                PileError.send(
                        pos, Component.translatable("marker.hbm.pile.intersect_core"), player);
                return false;
            }

            if (level.isClientSide()) return true;

            BlockPos owner =
                    level instanceof ServerLevel server
                            ? AssembledMembers.owner(server, pos)
                            : null;
            PileCoreBlockEntity core =
                    owner == null
                            ? null
                            : ChunkUtil.blockEntityIfLoaded(
                                    PileCoreBlockEntity.class, level, owner);
            if (core != null && side != null) {
                return core.drillChannel(pos, side.getOpposite(), player);
            }

            PileError.send(pos, Component.translatable("marker.hbm.pile.no_core"), player);
        }

        return false;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        Role role = level.getBlockState(pos).getValue(ROLE);
        String key =
                switch (role) {
                    case FUEL_IN -> "overlay.hbm.pile.fuel_in";
                    case FUEL_OUT -> "overlay.hbm.pile.fuel_out";
                    case AIR_IN -> "overlay.hbm.pile.air_in";
                    case AIR_OUT -> "overlay.hbm.pile.air_out";
                    case CONTROL -> "overlay.hbm.pile.control";
                    default -> null;
                };
        if (key == null && role != Role.CORE) return;
        info.title(getName());
        if (key != null) info.row(Component.empty().append(Component.translatable(key)));
        if (role == Role.CORE && level.getBlockEntity(pos) instanceof PileCoreBlockEntity core)
            info.row(
                    Component.empty()
                            .append(
                                    Component.translatable(
                                            "overlay.hbm.pile.core.temp",
                                            Math.round(core.highestHeat),
                                            PileCoreBlockEntity.MAX_HEAT)));
    }

    public enum Role implements StringRepresentable {
        DUMMY("dummy"),
        CORE("core"),
        CHANNEL("channel"),
        FUEL_IN("fuel_in"),
        FUEL_OUT("fuel_out"),
        AIR_IN("air_in"),
        AIR_OUT("air_out"),
        CONTROL("control"),
        EDGE("edge");

        private final String name;

        Role(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    @Override
    public boolean bulkMemberCell(BlockState state) {
        return state.getValue(ROLE) != Role.CORE;
    }

    public static void relocateBulkData(CompoundTag tag, BulkTransform transform) {
        PileCoreBlockEntity.relocateBulkData(tag, transform);
    }

    public static void transformBulkData(CompoundTag tag, BulkTransform transform) {
        PileCoreBlockEntity.transformBulkData(tag, transform);
    }

    public static CompoundTag publicBulkData(CompoundTag tag) {
        return PileCoreBlockEntity.publicBulkData(tag);
    }
}
