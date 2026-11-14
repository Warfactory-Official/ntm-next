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

import com.hbm.blockentity.machine.ThresherBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ThresherBlock extends HorizontalMachineBlock
        implements ToolInteractionHandler, LookOverlayProvider, CapabilityBlock {

    public ThresherBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ThresherBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!(stack.getItem() instanceof FluidIdentifierItem) || player.isShiftKeyDown())
            return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof ThresherBlockEntity be))
            return InteractionResult.PASS;

        FluidIdentifierData data =
                stack.getOrDefault(
                        ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
        Fluid type = data.primary();
        if (type == null
                || type == Fluids.EMPTY
                || !ThresherBlockEntity.acceptsFuel(NTMFluidProperties.kindOf(type)))
            return InteractionResult.PASS;

        be.tank.setTankTypeByIdentifier(type);
        be.setChanged();
        player.sendSystemMessage(
                Component.translatable(
                        "hbm.message.thresherSet", NTMFluidProperties.getDisplayName(type)));
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean onToolUse(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (level.isClientSide()) return true;
        if (!(level.getBlockEntity(pos) instanceof ThresherBlockEntity be)) return false;
        be.toggleSuspended();
        be.networkPackNT(25);
        return true;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        if (!(level.getBlockEntity(pos) instanceof ThresherBlockEntity be)) return;
        info.title(getName());
        info.row(
                Component.translatable(
                        "overlay.hbm.tank",
                        NTMFluidProperties.clientName(be.tank.getFluid()),
                        be.tank.getFill(),
                        be.tank.getMaxFill()));
        if (be.isSuspended)
            info.row(
                    Component.empty()
                            .append(Component.literal("! "))
                            .append(Component.translatable(getDescriptionId() + ".suspended"))
                            .append(Component.literal(" !")),
                    0xFF5555);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.THRESHER)
                .fluidIn()
                .faces(ThresherBlockEntity.class, ThresherBlockEntity::acceptsFace);
    }
}
