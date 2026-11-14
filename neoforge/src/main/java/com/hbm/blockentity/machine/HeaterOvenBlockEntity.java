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

package com.hbm.blockentity.machine;

import com.hbm.api.tile.HeatHandler;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ContractLink;
import com.hbm.capability.NtmContracts;
import com.hbm.data.MachineData;
import com.hbm.inventory.container.HeaterOvenMenu;
import com.hbm.modules.ModuleBurnTime;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

public class HeaterOvenBlockEntity extends FireboxBaseBlockEntity implements MenuProvider {
    private static final ModuleBurnTime BURN_MODULE =
            new ModuleBurnTime()
                    .setLigniteTimeMod(1.25)
                    .setCoalTimeMod(1.25)
                    .setCokeTimeMod(1.25)
                    .setSolidTimeMod(1.5)
                    .setRocketTimeMod(1.5)
                    .setBalefireTimeMod(0.5)
                    .setLigniteHeatMod(2)
                    .setCoalHeatMod(2)
                    .setCokeHeatMod(2)
                    .setSolidHeatMod(3)
                    .setRocketHeatMod(5)
                    .setBalefireHeatMod(15);

    private final ContractLink<HeatHandler> heatBelow =
            new ContractLink<>(NtmContracts.HEAT_SOURCE);

    public HeaterOvenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEATING_OVEN.get(), pos, state);
    }

    @Override
    public void tickServer() {
        tryPullHeat();
        super.tickServer();
    }

    private void tryPullHeat() {
        BlockPos heatPos = worldPosition.below();
        HeatHandler source = heatBelow.get(level, heatPos);
        if (source == null) return;

        int toPull =
                Math.max(
                        Math.min(
                                source.getHeatStored(),
                                MachineData.HEATER_OVEN_MAX_HEAT_ENERGY.get() - this.heatEnergy),
                        0);
        this.hbm$syncSet$FireboxBaseBlockEntity$heatEnergy(
                (int)
                        (this.heatEnergy
                                + ((int) (toPull * MachineData.HEATER_OVEN_HEAT_EFF.get()))));
        if (toPull > 0) markChanged();
        source.drainHeat(toPull);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.heaterOven");
    }

    @Override
    public ModuleBurnTime getModule() {
        return BURN_MODULE;
    }

    @Override
    public int getBaseHeat() {
        return MachineData.HEATER_OVEN_BASE_HEAT.get();
    }

    @Override
    public double getTimeMult() {
        return MachineData.HEATER_OVEN_TIME_MULT.get();
    }

    @Override
    public int getMaxHeat() {
        return MachineData.HEATER_OVEN_MAX_HEAT_ENERGY.get();
    }

    public List<String> getFuelBonuses() {
        return BURN_MODULE.getDesc();
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new HeaterOvenMenu(containerId, playerInventory, this);
    }

    public void refreshHeatBelow() {
        heatBelow.refresh(level, worldPosition.below());
    }

    @java.lang.Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        heatBelow.load(input, "heatBelow");
    }

    @java.lang.Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        heatBelow.save(output, "heatBelow");
    }
}
