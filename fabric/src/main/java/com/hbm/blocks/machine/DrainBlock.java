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

import com.hbm.blockentity.machine.DrainBlockEntity;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class DrainBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, LookOverlayProvider, CapabilityBlock {
    public DrainBlock(Properties props) {
        super(props);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useItemOnAtCore(
            ItemStack stack,
            BlockState coreState,
            Level level,
            BlockPos core,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (player.isShiftKeyDown() || !(stack.getItem() instanceof FluidIdentifierItem)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (!(level.getBlockEntity(core) instanceof DrainBlockEntity drain))
            return InteractionResult.PASS;

        FluidIdentifierData data =
                stack.getOrDefault(
                        ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
        Fluid target = data.primary();
        if (target == Fluids.EMPTY) return InteractionResult.SUCCESS;

        drain.tank.setTankTypeByIdentifier(target);
        drain.setChanged();
        player.sendSystemMessage(
                Component.translatable("desc.shared.changedTypeTo")
                        .withStyle(ChatFormatting.YELLOW)
                        .append(NTMFluidProperties.getDisplayName(target))
                        .append("!"));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        if (!(level.getBlockEntity(pos) instanceof DrainBlockEntity drain)) return;
        if (drain.tank.getTankType() == null) return;

        info.title(getName());
        info.row(
                LookOverlay.inflow(
                        Component.translatable(
                                "overlay.hbm.tank",
                                NTMFluidProperties.clientName(drain.tank.getFluid()),
                                drain.tank.getFill(),
                                drain.tank.getMaxFill())));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DrainBlockEntity(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed()
                .fluidIn()
                .fluidFaces(
                        DrainBlockEntity.class,
                        (be, face) ->
                                face.side() == null || face.side().getAxis() != Direction.Axis.Y);
    }
}
