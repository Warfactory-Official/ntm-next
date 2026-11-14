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

import com.hbm.blockentity.machine.storage.FluidBarrelBlockEntity;
import com.hbm.blockentity.machine.storage.OrbusBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.PersistentItemTooltipProvider;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class OrbusBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, CapabilityBlock, PersistentItemTooltipProvider {
    public OrbusBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useItemOnAtCore(
            ItemStack held,
            BlockState coreState,
            Level level,
            BlockPos core,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!(held.getItem() instanceof FluidIdentifierItem) || player.isShiftKeyDown()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(core) instanceof OrbusBlockEntity be))
            return InteractionResult.PASS;

        FluidIdentifierData data =
                held.getOrDefault(
                        ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
        Fluid type = data.primary();
        if (type == null || type == Fluids.EMPTY) return InteractionResult.PASS;

        be.tank.setTankTypeByIdentifier(type);
        be.setChanged();
        player.sendSystemMessage(
                Component.translatable("desc.shared.changedTypeTo")
                        .append(NTMFluidProperties.getDisplayName(type))
                        .append("!")
                        .withStyle(ChatFormatting.YELLOW));
        return InteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof MenuProvider menu) {
            openCoreMenu(player, core, menu);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OrbusBlockEntity(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.ORBUS)
                .fluidIn()
                .fluidOut()
                .items()
                .fluidFaces(
                        FluidBarrelBlockEntity.class,
                        (be, face) -> face.fluid() == null || be.connectsFluid(face.fluid()));
    }

    @Override
    public void appendPersistentInfo(ItemStack stack, Consumer<Component> adder) {
        FluidStackNTM content = stack.get(ModDataComponents.FLUID_CONTENT.get());
        if (content == null) return;
        adder.accept(PersistentItemTooltipProvider.tankLine(content, OrbusBlockEntity.CAPACITY));
    }
}
