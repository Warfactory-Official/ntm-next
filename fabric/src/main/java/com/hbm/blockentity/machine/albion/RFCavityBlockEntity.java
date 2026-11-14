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

package com.hbm.blockentity.machine.albion;

import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.machine.albion.PASourceBlockEntity.Particle;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.inventory.BlockMenus;
import com.hbm.inventory.container.PARFCMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class RFCavityBlockEntity extends CooledAcceleratorBlockEntity
        implements AcceleratorComponent, RadioValueProvider, MenuProvider {

    public static final int BATTERY_SLOT = 0;
    public static final int INVENTORY_SIZE = 1;

    private static final long ENERGY_CAPACITY = 1_000_000L;
    private static final long ENTRY_COST = 250_000L;
    private static final String[] RADIO_DESCRIPTORS = {
        RadioCommandParser.VAL + "temperature",
        RadioCommandParser.VAL + "pfmcold",
        RadioCommandParser.VAL + "pfm"
    };

    public RFCavityBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PA_RFC.get(), pos, state, INVENTORY_SIZE);
    }

    @Override
    public long getMaxPower() {
        return ENERGY_CAPACITY;
    }

    @Override
    public void tickServer() {
        long discharged =
                ItemEnergyTransfer.extract(this, BATTERY_SLOT, getMaxPower() - power, false);
        if (discharged != 0)
            hbm$syncSet$CooledAcceleratorBlockEntity$power((long) (power + (discharged)));
        super.tickServer();
    }

    private Direction beamDirection() {
        return MultiblockCoreBlock.coreFacing(getBlockState()).getCounterClockWise();
    }

    @Override
    public boolean canAcceptParticle(Particle particle, Direction travel, BlockPos cell) {
        var beam = beamDirection();
        return travel == beam
                && cell.getY() == worldPosition.getY()
                && cell.getX() == worldPosition.getX() - 4 * beam.getStepX()
                && cell.getZ() == worldPosition.getZ() - 4 * beam.getStepZ();
    }

    @Override
    public void onParticleEnter(Particle particle, Direction travel) {
        if (admit(particle, ENTRY_COST, null)) {
            particle.addDistance(9);
            particle.momentum += 100;
            particle.defocus(100);
        }
    }

    @Override
    public BlockPos getParticleExit(Particle particle) {
        return worldPosition.relative(beamDirection(), 5);
    }

    @Override
    public boolean stillValid(Player player) {
        return BlockMenus.withinTwelveBlocks(this, player);
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new PARFCMenu(containerId, playerInventory, this);
    }

    @Override
    public String[] getFunctionInfo() {
        return RADIO_DESCRIPTORS;
    }

    @Override
    public @Nullable String getRadioValue(String key) {
        return switch (key) {
            case RadioCommandParser.VAL + "temperature" -> Integer.toString((int) temperature);
            case RadioCommandParser.VAL + "pfmcold" -> Integer.toString(coolantTanks[0].getFill());
            case RadioCommandParser.VAL + "pfm" -> Integer.toString(coolantTanks[1].getFill());
            default -> null;
        };
    }
}
