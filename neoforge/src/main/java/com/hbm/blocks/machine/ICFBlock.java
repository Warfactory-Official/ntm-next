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

import com.hbm.api.energymk2.EnergyCaps;
import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.blockentity.machine.ICFControllerBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.platform.Caps;
import com.hbm.registration.RegistryHandle;
import com.hbm.util.ChunkUtil;
import com.hbm.world.bulk.BulkAction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

public class ICFBlock extends Block implements CapabilityBlock, AssembledMembers.Member {

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    public ICFBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, Part.CASING));
    }

    public static BlockState shellOf(BlockState original) {
        var part = Part.of(original.getBlock());
        return ModBlocks.ICF_BLOCK.get().defaultBlockState().setValue(PART, part);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (BulkAction.active()) {
            return;
        }

        var owner = AssembledMembers.owner(level, pos);
        AssembledMembers.release(level, pos);
        var original = state.getValue(PART).original();
        if (level.getBlockState(pos) == original) {
            return;
        }

        level.setBlock(pos, original, Block.UPDATE_ALL);
        if (owner != null) {
            var controller =
                    ChunkUtil.blockEntityIfLoaded(ICFControllerBlockEntity.class, level, owner);
            if (controller != null) {
                controller.disassemble();
            }
        }
    }

    @Override
    public void verify(ServerLevel level, BlockPos pos, BlockState state, BlockPos owner) {
        var controller =
                ChunkUtil.blockEntityIfLoaded(ICFControllerBlockEntity.class, level, owner);
        if (controller != null && controller.isAssembled()) {
            return;
        }
        level.setBlock(pos, state.getValue(PART).original(), Block.UPDATE_ALL);
    }

    @Override
    public int endpointDomains(BlockState state) {
        if (state.getValue(PART) == Part.PORT) {
            return MachineCaps.POWER_IN;
        }
        return 0;
    }

    @Override
    public boolean hasPowerContact(Level level, BlockPos pos, BlockState state, Direction side) {
        return state.getValue(PART) == Part.PORT;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().powerIn().selfProvided();
    }

    @Override
    public void declareExtraCaps(RegistryHandle<? extends Block> self) {
        Caps.pendingBlocks.add(
                new Caps.BlockRegistration<>(
                        EnergyCaps.RECEIVER, (Supplier<? extends Block>) self, ICFBlock::receiver));
    }

    private static @Nullable EnergyHandler receiver(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity,
            @Nullable Direction side) {
        if (side == null
                || state.getValue(PART) != Part.PORT
                || !(level instanceof ServerLevel server)) {
            return null;
        }

        var owner = AssembledMembers.owner(server, pos);
        if (owner == null) {
            return null;
        }
        return ChunkUtil.blockEntityIfLoaded(ICFControllerBlockEntity.class, server, owner);
    }

    public enum Part implements StringRepresentable {
        CASING("casing", () -> ModBlocks.ICF_CASING.get()),
        PORT("port", () -> ModBlocks.ICF_PORT.get()),
        CELL("cell", () -> ModBlocks.ICF_CELL.get()),
        EMITTER("emitter", () -> ModBlocks.ICF_EMITTER.get()),
        CAPACITOR("capacitor", () -> ModBlocks.ICF_CAPACITOR.get()),
        TURBOCHARGER("turbocharger", () -> ModBlocks.ICF_TURBOCHARGER.get());

        private static final Part[] PARTS = values();

        private final String serializedName;
        private final Supplier<? extends Block> originalBlock;

        Part(String serializedName, Supplier<? extends Block> originalBlock) {
            this.serializedName = serializedName;
            this.originalBlock = originalBlock;
        }

        public static Part of(Block original) {
            for (var part : PARTS) {
                if (part.originalBlock.get() == original) {
                    return part;
                }
            }
            throw new IllegalArgumentException();
        }

        public BlockState original() {
            return originalBlock.get().defaultBlockState();
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
