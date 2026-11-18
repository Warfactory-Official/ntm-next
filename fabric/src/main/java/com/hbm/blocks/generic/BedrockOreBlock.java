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

import com.hbm.blockentity.BedrockOreBlockEntity;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.DrillbitItem;
import com.hbm.items.machine.FluidContainerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class BedrockOreBlock extends Block implements EntityBlock, LookOverlayProvider {
    public BedrockOreBlock(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BedrockOreBlockEntity(pos, state);
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
        if (stack.isEmpty() || !player.hasInfiniteMaterials()) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof BedrockOreBlockEntity ore) {
            Fluid contained = containedFluid(stack);
            if (stack.getItem() instanceof DrillbitItem drill) {
                ore.setData(ore.resource, ore.acid, ore.color, drill.type.tier, ore.shape);
            } else if (contained != null) {
                ore.setData(
                        ore.resource,
                        new FluidStackNTM(
                                contained, FluidTankNTM.containerContent(stack, contained)),
                        ore.color,
                        ore.tier,
                        ore.shape);
            } else if (stack.getItem() instanceof FluidContainerItem item
                    && item.machineFillable()) {
                Fluid type = item.firstFluidType(stack);
                if (type != null) {
                    ore.setData(
                            ore.resource,
                            new FluidStackNTM(type, item.getFill(stack)),
                            ore.color,
                            ore.tier,
                            ore.shape);
                }
            } else {
                ore.setData(
                        stack.copy(), ore.acid, ore.color, ore.tier, level.getRandom().nextInt(10));
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static @Nullable Fluid containedFluid(ItemStack stack) {
        for (Fluid fluid : NTMFluids.displayOrder()) {
            if (FluidTankNTM.containerContent(stack, fluid) > 0) return fluid;
        }
        return null;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        if (!(level.getBlockEntity(pos) instanceof BedrockOreBlockEntity ore)) return;
        info.title(getName());
        if (!ore.resource.isEmpty())
            info.row(Component.empty().append(ore.resource.getHoverName()));
        info.row(
                Component.empty()
                        .append(Component.translatable("hbm.desc.bedrock_tier", ore.tier)));
        if (ore.acid != null) {
            info.row(
                    Component.empty()
                            .append(
                                    Component.translatable(
                                            "hbm.desc.bedrock_requires",
                                            ore.acid.amount(),
                                            NTMFluidProperties.clientName(ore.acid.type()))));
        }
    }
}
