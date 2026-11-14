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

package com.hbm.items.machine;

import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.PipeData;
import com.hbm.blocks.network.FluidDuctBlock;
import com.hbm.blocks.network.FluidPipeBlock;
import com.hbm.inventory.container.HeldItemMenu;
import com.hbm.inventory.container.ModMenus;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.ModDataComponents;
import com.hbm.items.StackRemainderItem;
import com.hbm.sound.EntitySounds;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class FluidIdentifierItem extends StackRemainderItem {

    public FluidIdentifierItem(Item.Properties props) {
        super(props);
    }

    @Override
    protected ItemStackTemplate remainder(ItemStack consumed) {
        return ItemStackTemplate.fromNonEmptyStack(consumed.copyWithCount(1));
    }

    private static Component displayName(Fluid type) {
        if (type == Fluids.EMPTY) return Component.translatable("hbmfluid.none");
        return NTMFluidProperties.getDisplayName(type);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FluidDuctBlock duct) || !duct.retypable())
            return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ItemStack stack = context.getItemInHand();
        FluidIdentifierData data =
                stack.getOrDefault(
                        ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
        Fluid handFluid = data.primary();

        ServerLevel serverLevel = (ServerLevel) level;
        long key = pos.asLong();
        PipeData pipeData = FluidPipeGraph.dataAt(serverLevel, key);
        Fluid pipeFluid = pipeData == null ? Fluids.EMPTY : pipeData.fluid();

        if (handFluid == Fluids.EMPTY) {
            if (pipeFluid == Fluids.EMPTY) return InteractionResult.SUCCESS;
            stack.set(ModDataComponents.FLUID_IDENTIFIER.get(), data.withPrimary(pipeFluid));
            level.playSound(
                    null,
                    pos,
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS,
                    0.25F,
                    0.75F);
            return InteractionResult.CONSUME;
        }

        if (handFluid == pipeFluid) return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            FluidPipeBlock.retypeRun(serverLevel, key, handFluid);
        } else {
            FluidPipeBlock.retypeNode(serverLevel, key, handFluid);
            FluidPipeBlock.refreshAround(serverLevel, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            HeldItemMenu.open(
                    player,
                    hand,
                    ModMenus.FLUID_IDENTIFIER,
                    Component.translatable("item.hbm.fluid_identifier_multi"));
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            ItemStack stack = player.getItemInHand(hand);
            FluidIdentifierData data =
                    stack.getOrDefault(
                            ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
            stack.set(ModDataComponents.FLUID_IDENTIFIER.get(), data.swap());
            EntitySounds.play(
                    player,
                    player.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS,
                    0.25F,
                    1.25F);
            if (player instanceof ServerPlayer sp) {
                sp.sendSystemMessage(displayName(data.secondary()), true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        FluidIdentifierData data =
                stack.getOrDefault(
                        ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
        adder.accept(Component.translatable("item.hbm.fluid_identifier_multi.info"));
        adder.accept(Component.literal("   ").append(displayName(data.primary())));
        adder.accept(Component.translatable("item.hbm.fluid_identifier_multi.info2"));
        adder.accept(Component.literal("   ").append(displayName(data.secondary())));
    }
}
