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

import com.hbm.blockentity.machine.ICFControllerBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.MarkerLayout;
import com.hbm.util.NumberFormatter;
import com.hbm.world.bulk.BulkTransform;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class ICFControllerBlock extends Block
        implements TickingEntityBlock, LookOverlayProvider, CapabilityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final int MAX_SIZE = 1024;

    public ICFControllerBlock(Properties props) {
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
        return new ICFControllerBlockEntity(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.ICF_CONTROLLER).powerIn().fe();
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof ICFControllerBlockEntity controller))
            return InteractionResult.PASS;

        if (!controller.isAssembled()) assemble(level, pos, state, player);
        return InteractionResult.SUCCESS;
    }

    private void assemble(Level level, BlockPos pos, BlockState state, Player player) {
        Map<BlockPos, BlockState> assembly = new HashMap<>();
        Set<BlockPos> ports = new HashSet<>();
        Set<BlockPos> cells = new HashSet<>();
        Set<BlockPos> emitters = new HashSet<>();
        Set<BlockPos> capacitors = new HashSet<>();
        Set<BlockPos> turbochargers = new HashSet<>();

        assembly.put(pos, state);
        Direction dir = state.getValue(FACING).getOpposite();

        ArrayDeque<BlockPos> stack = new ArrayDeque<>();
        stack.push(pos.relative(dir));

        Map<BlockPos, Component> errors = new LinkedHashMap<>();
        while (!stack.isEmpty()) {
            BlockPos p = stack.pop();
            if (assembly.containsKey(p)) continue;
            if (assembly.size() >= MAX_SIZE) {
                errors.put(p, Component.translatable("marker.hbm.max_size"));
                continue;
            }

            BlockState s = level.getBlockState(p);
            Block b = s.getBlock();

            if (b == ModBlocks.ICF_CASING.get()) {
                assembly.put(p, s);
                continue;
            }
            if (b == ModBlocks.ICF_PORT.get()) {
                assembly.put(p, s);
                ports.add(p);
                continue;
            }
            if (b == ModBlocks.ICF_CELL.get()
                    || b == ModBlocks.ICF_EMITTER.get()
                    || b == ModBlocks.ICF_CAPACITOR.get()
                    || b == ModBlocks.ICF_TURBOCHARGER.get()) {
                assembly.put(p, s);
                if (b == ModBlocks.ICF_CELL.get()) cells.add(p);
                else if (b == ModBlocks.ICF_EMITTER.get()) emitters.add(p);
                else if (b == ModBlocks.ICF_CAPACITOR.get()) capacitors.add(p);
                else turbochargers.add(p);
                stack.push(p.north());
                stack.push(p.south());
                stack.push(p.below());
                stack.push(p.above());
                stack.push(p.west());
                stack.push(p.east());
                continue;
            }

            errors.put(p, Component.translatable("marker.hbm.icf.non_laser"));
        }

        if (!errors.isEmpty()) {
            if (player instanceof ServerPlayer sp) {
                PacketWire.sendTo(MarkerLayout.encode(0xff0000, 5000, 128.0, errors), sp);
            }
            return;
        }

        List<BlockPos> members = new ArrayList<>(assembly.size());
        for (Map.Entry<BlockPos, BlockState> entry : assembly.entrySet()) {
            BlockPos partPos = entry.getKey();
            if (partPos.equals(pos)) continue;
            level.setBlock(partPos, ICFBlock.shellOf(entry.getValue()), 3);
            members.add(partPos);
        }
        AssembledMembers.assemble((ServerLevel) level, pos, members);

        if (level.getBlockEntity(pos) instanceof ICFControllerBlockEntity controller) {
            controller.configureAssembly(ports, cells, emitters, capacitors, turbochargers);
            controller.assemble(
                    BoundingBox.encapsulatingPositions(assembly.keySet()).orElseThrow());
        }
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        if (!(level.getBlockEntity(pos) instanceof ICFControllerBlockEntity icf)) return;
        info.title(getName());
        info.row(
                Component.translatable(
                        "overlay.hbm.charge_tight",
                        NumberFormatter.formatCompact(icf.getPower()),
                        NumberFormatter.formatCompact(icf.getMaxPower())));
    }

    public static void relocateBulkData(CompoundTag tag, BulkTransform transform) {
        ICFControllerBlockEntity.relocateBulkData(tag, transform);
    }

    public static CompoundTag publicBulkData(CompoundTag tag) {
        return ICFControllerBlockEntity.publicBulkData(tag);
    }
}
