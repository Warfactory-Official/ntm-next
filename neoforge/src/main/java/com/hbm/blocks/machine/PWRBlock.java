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

import com.hbm.api.fluidmk2.FluidCaps;
import com.hbm.api.fluidmk2.FluidHandler;
import com.hbm.blockentity.machine.PWRControllerBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.capability.NtmContracts.Contract;
import com.hbm.capability.NtmContracts;
import com.hbm.platform.Caps;
import com.hbm.registration.RegistryHandle;
import com.hbm.util.ChunkUtil;
import com.hbm.world.bulk.BulkAction;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class PWRBlock extends Block implements CapabilityBlock, AssembledMembers.Member {

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    public PWRBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(PART, Part.CASING));
    }

    public static BlockState shellOf(BlockState original) {
        return ModBlocks.PWR_BLOCK
                .get()
                .defaultBlockState()
                .setValue(PART, Part.of(original.getBlock()));
    }

    public static void declareProxies() {
        NtmContracts.proxy(
                NtmContracts.INVENTORY,
                ModBlocks.PWR_BLOCK,
                (level, pos, state) -> {
                    PWRControllerBlockEntity core = ioController(level, pos, state);
                    return core == null ? null : core.getBlockPos();
                });
        for (Contract<?> ror :
                List.of(NtmContracts.ROR_VALUE_PROVIDER, NtmContracts.ROR_INTERACTIVE)) {
            NtmContracts.proxy(
                    ror,
                    ModBlocks.PWR_BLOCK,
                    (level, pos, state) -> {
                        PWRControllerBlockEntity core =
                                level instanceof ServerLevel
                                        ? ioController(level, pos, state)
                                        : walkToController(level, pos, state);
                        return core == null ? null : core.getBlockPos();
                    });
        }
    }

    public static @Nullable PWRControllerBlockEntity ioController(
            Level level, BlockPos pos, BlockState state) {
        if (!isPort(state)) return null;
        if (!(level instanceof ServerLevel server)) return null;
        BlockPos owner = AssembledMembers.owner(server, pos);
        return owner == null
                ? null
                : ChunkUtil.blockEntityIfLoaded(PWRControllerBlockEntity.class, level, owner);
    }

    public static @Nullable PWRControllerBlockEntity walkToController(
            Level level, BlockPos port, BlockState state) {
        if (!isPort(state)) return null;
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        seen.add(port);
        open.add(port);
        while (!open.isEmpty()) {
            BlockPos at = open.poll();
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos candidate = at.relative(side);
                BlockState controller = level.getBlockState(candidate);
                if (controller.getBlock() instanceof PWRControllerBlock
                        && controller.getValue(PWRControllerBlock.FACING) == side
                        && level.getBlockEntity(candidate) instanceof PWRControllerBlockEntity be
                        && be.assembled) {
                    return be;
                }
            }
            for (Direction side : Direction.VALUES) {
                BlockPos next = at.relative(side);
                BlockState shell = level.getBlockState(next);
                if (shell.getBlock() instanceof PWRBlock
                        && PWRControllerBlock.isValidCore(
                                shell.getValue(PART).original().getBlock())
                        && seen.add(next)) {
                    open.add(next);
                }
            }
        }
        return null;
    }

    private static boolean isPort(BlockState state) {
        return state.getBlock() instanceof PWRBlock && state.getValue(PART) == Part.PORT;
    }

    private static @Nullable FluidHandler portCap(
            Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
        return side == null ? null : ioController(level, pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (BulkAction.active()) return;
        BlockPos owner = AssembledMembers.owner(level, pos);
        AssembledMembers.release(level, pos);
        BlockState original = state.getValue(PART).original();
        if (level.getBlockState(pos) == original) return;
        level.setBlock(pos, original, UPDATE_ALL);
        PWRControllerBlockEntity controller =
                owner == null
                        ? null
                        : ChunkUtil.blockEntityIfLoaded(
                                PWRControllerBlockEntity.class, level, owner);
        if (controller != null) controller.disassemble();
    }

    @Override
    public void verify(ServerLevel level, BlockPos pos, BlockState state, BlockPos owner) {
        if (level.getBlockEntity(owner) instanceof PWRControllerBlockEntity controller
                && controller.assembled) {
            return;
        }
        level.setBlock(pos, state.getValue(PART).original(), UPDATE_ALL);
    }

    @Override
    public int endpointDomains(BlockState state) {
        return state.getValue(PART) == Part.PORT ? MachineCaps.FLUID_IN | MachineCaps.FLUID_OUT : 0;
    }

    @Override
    public boolean hasFluidContact(
            Level level, BlockPos pos, BlockState state, Direction side, Fluid fluid) {
        if (!isPort(state)) return false;
        if (level instanceof ServerLevel && ioController(level, pos, state) == null) return true;
        return CapabilityBlock.super.hasFluidContact(level, pos, state, side, fluid);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().itemsAtCells();
    }

    @Override
    public void declareExtraCaps(RegistryHandle<? extends Block> self) {
        Caps.pendingBlocks.add(
                new Caps.BlockRegistration<>(
                        FluidCaps.RECEIVER,
                        (Supplier<? extends Block>) self,
                        (level, pos, state, be, face) -> portCap(level, pos, state, face.side())));
        Caps.pendingBlocks.add(
                new Caps.BlockRegistration<>(
                        FluidCaps.PROVIDER,
                        (Supplier<? extends Block>) self,
                        (level, pos, state, be, face) -> portCap(level, pos, state, face.side())));
    }

    public enum Part implements StringRepresentable {
        CASING("casing", () -> ModBlocks.PWR_CASING.get()),
        PORT("port", () -> ModBlocks.PWR_PORT.get()),
        REFLECTOR("reflector", () -> ModBlocks.PWR_REFLECTOR.get()),
        FUEL("fuel", () -> ModBlocks.PWR_FUEL.get()),
        CONTROL("control", () -> ModBlocks.PWR_CONTROL.get()),
        CHANNEL("channel", () -> ModBlocks.PWR_CHANNEL.get()),
        HEATEX("heatex", () -> ModBlocks.PWR_HEATEX.get()),
        HEATSINK("heatsink", () -> ModBlocks.PWR_HEATSINK.get()),
        NEUTRON_SOURCE("neutron_source", () -> ModBlocks.PWR_NEUTRON_SOURCE.get());

        private final String name;
        private final Supplier<Block> block;

        Part(String name, Supplier<Block> block) {
            this.name = name;
            this.block = block;
        }

        public static Part of(Block original) {
            for (Part part : values()) {
                if (part.block.get() == original) return part;
            }
            throw new IllegalArgumentException(String.valueOf(original));
        }

        public BlockState original() {
            return block.get().defaultBlockState();
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
