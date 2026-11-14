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

package com.hbm.blockentity.machine.albion;

import com.hbm.api.energymk2.EnergyItem;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.machine.albion.AcceleratorComponent;
import com.hbm.blockentity.machine.albion.CooledAcceleratorBlockEntity;
import com.hbm.blockentity.machine.albion.PASourceBlockEntity.PAState;
import com.hbm.blockentity.machine.albion.PASourceBlockEntity.Particle;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.inventory.container.PAQuadrupoleMenu;
import com.hbm.items.machine.PACoilItem.EnumCoilType;
import com.hbm.items.machine.PACoilItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import static com.hbm.api.redstoneoverradio.RadioCommandParser.VAL;

public class PAQuadrupoleBlockEntity extends CooledAcceleratorBlockEntity
        implements MenuProvider, AcceleratorComponent, RadioValueProvider {
    public static final String[] ROR = {VAL + "temperature", VAL + "pfmcold", VAL + "pfm"};
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_COIL = 1;
    public static final int SLOT_COUNT = 2;

    public static final long usage = 100_000;
    public static final int focusGain = 100;

    public PAQuadrupoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PA_QUADRUPOLE.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public long getMaxPower() {
        return 2_500_000;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.paQuadrupole");
    }

    private Direction beamlineDir() {
        return MultiblockCoreBlock.coreFacing(getBlockState()).getCounterClockWise();
    }

    @Override
    public boolean canAcceptParticle(Particle particle, Direction dir, BlockPos pos) {
        Direction beamlineDir = beamlineDir();
        return worldPosition.relative(beamlineDir, -1).equals(pos) && beamlineDir == dir;
    }

    @Override
    public void onParticleEnter(Particle particle, Direction dir) {
        @Nullable EnumCoilType type = PACoilItem.typeOf(inventory.get(SLOT_COIL));
        long cost = type != null && type.quadMin > particle.momentum ? usage * 10 : usage;
        @Nullable PAState fault =
                type == null
                        ? PAState.CRASH_NOCOIL
                        : type.quadMax < particle.momentum ? PAState.CRASH_OVERSPEED : null;
        if (!admit(particle, cost, fault)) return;
        particle.addDistance(3);
        particle.focus(focusGain);
    }

    @Override
    public BlockPos getParticleExit(Particle particle) {
        return worldPosition.relative(beamlineDir(), 2);
    }

    @Override
    public void tickServer() {
        hbm$syncSet$CooledAcceleratorBlockEntity$power(
                (long)
                        (power
                                + (ItemEnergyTransfer.extract(
                                        this, SLOT_BATTERY, getMaxPower() - power, false))));
        super.tickServer();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> EnergyItem.matches(stack);
            case SLOT_COIL -> stack.getItem() instanceof PACoilItem;
            default -> false;
        };
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public String getRadioValue(String name) {
        if ((VAL + "temperature").equals(name)) return Integer.toString((int) temperature);
        if ((VAL + "pfmcold").equals(name)) return Integer.toString(coolantTanks[0].getFill());
        if ((VAL + "pfm").equals(name)) return Integer.toString(coolantTanks[1].getFill());
        return null;
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new PAQuadrupoleMenu(containerId, playerInventory, this);
    }
}
