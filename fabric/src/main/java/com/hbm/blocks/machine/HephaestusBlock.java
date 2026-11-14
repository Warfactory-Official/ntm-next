/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.GeothermalExchanger;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class HephaestusBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, LookOverlayProvider, CapabilityBlock {
    public HephaestusBlock(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeothermalExchanger(pos, state);
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
        if (!(stack.getItem() instanceof FluidIdentifierItem)
                || player.isShiftKeyDown()
                || player.isSpectator()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(core) instanceof GeothermalExchanger be))
            return InteractionResult.PASS;

        FluidIdentifierData data =
                stack.getOrDefault(
                        ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
        Fluid type = data.primary();
        if (type == null || type == Fluids.EMPTY) return InteractionResult.PASS;

        be.selectInputFluid(type);
        player.sendSystemMessage(
                Component.translatable(
                        "hbm.message.hephaestusSet", NTMFluidProperties.getDisplayName(type)));
        return InteractionResult.CONSUME;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        if (!(level.getBlockEntity(pos) instanceof GeothermalExchanger be)) return;
        info.title(Component.empty().append(Component.translatable(getDescriptionId())));
        info.row(
                Component.translatable(
                        "unit.heat_spaced", String.format(Locale.ROOT, "%,d", be.heatRate)));

        FluidTankNTM[] tanks = {be.inputTank, be.outputTank};
        for (int i = 0; i < tanks.length; i++) {
            FluidTankNTM tank = tanks[i];
            String arrow = i == 0 ? ChatFormatting.GREEN + "-> " : ChatFormatting.RED + "<- ";
            info.row(
                    Component.literal(arrow)
                            .append(
                                    Component.translatable(
                                            "overlay.hbm.tank",
                                            NTMFluidProperties.clientName(tank.getFluid()),
                                            tank.getFill(),
                                            tank.getMaxFill())));
        }
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed()
                .fluidIn()
                .fluidOut()
                .fluidFaces(
                        GeothermalExchanger.class,
                        (be, face) ->
                                face.side() == null || face.side().getAxis() != Direction.Axis.Y);
    }
}
