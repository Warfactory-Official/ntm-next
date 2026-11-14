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

package com.hbm.blocks.machine.storage;

import com.hbm.blockentity.machine.storage.CapacitorBlockEntity;
import com.hbm.blockentity.machine.storage.CapacitorCharge;
import com.hbm.blocks.DirectionalBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.PersistentItemTooltipProvider;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.items.ModDataComponents;
import com.hbm.util.NumberFormatter;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jspecify.annotations.Nullable;

public class CapacitorBlock extends DirectionalBlock
        implements TickingEntityBlock,
                CapabilityBlock,
                PersistentItemTooltipProvider,
                LookOverlayProvider {

    public final long maxPower;
    private final MapCodec<CapacitorBlock> ownCodec;

    public CapacitorBlock(long maxPower, Properties props) {
        super(props);
        this.maxPower = maxPower;
        this.ownCodec = simpleCodec(p -> new CapacitorBlock(maxPower, p));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return ownCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getClickedFace());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CapacitorBlockEntity(pos, state, maxPower);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.CAPACITOR)
                .powerIn()
                .fe()
                .faces(CapacitorBlockEntity.class, CapacitorBlockEntity::acceptsFace);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        if (!(level.getBlockEntity(pos) instanceof CapacitorBlockEntity capacitor)) return;
        info.title(getName()).charge(capacitor.getPower(), capacitor.getMaxPower());
        info.row(
                Component.translatable(
                        "overlay.hbm.capacitor.received",
                        NumberFormatter.formatCompact(capacitor.powerReceivedThisTick())));
        info.row(
                Component.translatable(
                        "overlay.hbm.capacitor.sent",
                        NumberFormatter.formatCompact(capacitor.powerSentThisTick())));
    }

    @Override
    public void appendPersistentInfo(ItemStack stack, Consumer<Component> adder) {
        CapacitorCharge state = stack.get(ModDataComponents.CAPACITOR_STATE.get());
        if (state == null) return;
        adder.accept(
                Component.literal(
                                NumberFormatter.formatCompact(state.power())
                                        + "/"
                                        + NumberFormatter.formatCompact(state.maxPower())
                                        + "HE")
                        .withStyle(ChatFormatting.YELLOW));
    }
}
