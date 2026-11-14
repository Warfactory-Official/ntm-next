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

import com.hbm.blockentity.machine.thermal.FluidBoilerBlockEntity;
import com.hbm.interfaces.LookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.trait.HeatableTrait;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.util.NumberFormatter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;

public final class BoilerBlockActions {

    private BoilerBlockActions() {}

    public static InteractionResult configure(
            ItemStack held, Level level, BlockPos core, Player player, String messageKey) {
        if (!(held.getItem() instanceof FluidIdentifierItem) || player.isShiftKeyDown()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        var data =
                held.getOrDefault(
                        ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
        var fluid = data.primary();
        if (fluid == null || fluid == Fluids.EMPTY) return InteractionResult.PASS;

        var heating = NTMFluidProperties.getTrait(fluid, HeatableTrait.class);
        if (heating == null || heating.efficiency(HeatableTrait.Mode.BOILER) <= 0)
            return InteractionResult.PASS;
        var machine = (FluidBoilerBlockEntity) level.getBlockEntity(core);

        machine.receivingTank().setTankTypeByIdentifier(fluid);
        machine.setChanged();
        player.sendSystemMessage(
                Component.translatable(messageKey, NTMFluidProperties.getDisplayName(fluid)));
        return InteractionResult.CONSUME;
    }

    public static void readout(FluidBoilerBlockEntity machine, Component title, LookOverlay card) {
        var input = machine.receivingTank();
        var output = machine.deliveryTank();
        var inputText =
                Component.translatable(
                        "overlay.hbm.tank_spaced",
                        NTMFluidProperties.getDisplayName(input.getTankType()),
                        NumberFormatter.formatGrouped(input.getFill()),
                        NumberFormatter.formatGrouped(input.getMaxFill()));
        var outputText =
                Component.translatable(
                        "overlay.hbm.tank_spaced",
                        NTMFluidProperties.getDisplayName(output.getTankType()),
                        NumberFormatter.formatGrouped(output.getFill()),
                        NumberFormatter.formatGrouped(output.getMaxFill()));

        card.title(title);
        card.row(Component.translatable("unit.heat", NumberFormatter.formatGrouped(machine.heat)));
        card.row(LookOverlay.inflow(inputText));
        card.row(LookOverlay.outflow(outputText));
    }
}
