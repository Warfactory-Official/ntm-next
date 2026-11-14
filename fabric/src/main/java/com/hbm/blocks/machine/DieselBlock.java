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

import com.hbm.blockentity.machine.DieselGeneratorBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.BlockMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class DieselBlock extends HorizontalMachineBlock implements CapabilityBlock {

    public DieselBlock(Properties props) {
        super(props);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DieselGeneratorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            BlockMenus.open(player, provider);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof DieselGeneratorBlockEntity be)) return;
        if (!be.isOn || !be.hasAcceptableFuel() || be.tank.getFill() <= 0) return;

        Direction dir = state.getValue(FACING);
        Direction rot = dir.getClockWise();
        level.addParticle(
                ParticleTypes.SMOKE,
                pos.getX() + 0.5 - dir.getStepX() * 0.6 + rot.getStepX() * 0.1875,
                pos.getY() + 0.3125,
                pos.getZ() + 0.5 - dir.getStepZ() * 0.6 + rot.getStepZ() * 0.1875,
                0,
                0,
                0);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.DIESEL_GENERATOR)
                .powerOut()
                .fluidIn()
                .fluidOut()
                .items()
                .fe();
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
        if (hbm$be instanceof DieselGeneratorBlockEntity)
            ((DieselGeneratorBlockEntity) hbm$be).refreshRedstone();
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
        if (hbm$be instanceof DieselGeneratorBlockEntity)
            ((DieselGeneratorBlockEntity) hbm$be).refreshRedstone();
    }
}
