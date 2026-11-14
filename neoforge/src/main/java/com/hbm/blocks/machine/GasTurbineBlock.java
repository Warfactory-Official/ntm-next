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

import com.hbm.blockentity.machine.TurbineGasBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class GasTurbineBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, LookOverlayProvider, CapabilityBlock {
    public GasTurbineBlock(Properties props) {
        super(props);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurbineGasBlockEntity(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        BlockPos hit = info.hit();
        if (hit == null) return;
        if (!(level.getBlockEntity(pos) instanceof TurbineGasBlockEntity be)) return;

        Direction dir = MultiblockCoreBlock.coreFacing(level.getBlockState(pos));
        Direction rot = dir.getClockWise();
        BlockPos front = pos.relative(dir);
        BlockPos back = pos.relative(dir.getOpposite());

        if (hit.equals(back.relative(rot)) || hit.equals(front.relative(rot))) {
            info.row(LookOverlay.inflow(tankName(be.tanks[0])));
            info.row(LookOverlay.inflow(tankName(be.tanks[1])));
        }

        if (hit.equals(back.relative(rot, -4)) || hit.equals(front.relative(rot, -4))) {
            info.row(LookOverlay.inflow(tankName(be.tanks[2])));
        }

        if (hit.equals(pos.relative(rot, -5).above())) {
            info.row(LookOverlay.outflow(tankName(be.tanks[3])));
        }

        if (hit.equals(pos.relative(rot, 4).above())) {
            info.row(LookOverlay.outflow(Component.translatable("overlay.hbm.power_word")));
        }

        if (!info.isEmpty()) {
            info.title(getName());
        }
    }

    private static Component tankName(FluidTankNTM tank) {
        Fluid type = tank.getFluid();
        return type == null
                ? Component.translatable("overlay.hbm.none")
                : Component.literal(NTMFluidProperties.clientName(type));
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.MACHINE_GASTURBINE)
                .powerOut()
                .fluidIn()
                .fluidOut()
                .items()
                .fe()
                .faces(TurbineGasBlockEntity.class, (be, side) -> side != Direction.DOWN)
                .fluidFaces(
                        TurbineGasBlockEntity.class, (be, face) -> face.side() != Direction.DOWN);
    }
}
