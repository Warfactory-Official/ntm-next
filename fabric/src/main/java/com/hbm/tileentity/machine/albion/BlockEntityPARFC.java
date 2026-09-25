// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.albion;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.container.MenuPARFC;
import com.hbm.tileentity.machine.albion.BlockEntityPASource.PAState;
import com.hbm.tileentity.machine.albion.BlockEntityPASource.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityPARFC extends BlockEntityCooledBase
        implements MenuProvider, IParticleUser, IRORValueProvider {
    public static final String[] ROR = {
        PREFIX_VALUE + "temperature", PREFIX_VALUE + "pfmcold", PREFIX_VALUE + "pfm"
    };
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_COUNT = 1;

    public static final long usage = 250_000;
    public static final int momentumGain = 100;
    public static final int defocusGain = 100;

    public BlockEntityPARFC(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PA_RFC.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public long getMaxPower() {
        return 1_000_000;
    }

    private Direction beamlineDir() {
        return BlockMultiblockCore.coreFacing(getBlockState()).getCounterClockWise();
    }

    @Override
    public boolean canParticleEnter(Particle particle, Direction dir, BlockPos pos) {
        Direction rfcDir = beamlineDir();
        return worldPosition.relative(rfcDir, -4).equals(pos) && rfcDir == dir;
    }

    @Override
    public void onEnter(Particle particle, Direction dir) {
        if (!isCool()) particle.crash(PAState.CRASH_NOCOOL);
        if (this.power < usage) particle.crash(PAState.CRASH_NOPOWER);

        if (particle.invalid) return;

        particle.addDistance(9);
        particle.momentum += momentumGain;
        particle.defocus(defocusGain);
        this.power -= usage;
    }

    @Override
    public BlockPos getExitPos(Particle particle) {
        return worldPosition.relative(beamlineDir(), 5);
    }

    @Override
    public void tickServer() {
        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, getMaxPower() - power, false);
        super.tickServer();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_BATTERY && IBatteryItem.isBattery(stack);
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "temperature").equals(name)) return Integer.toString((int) temperature);
        if ((PREFIX_VALUE + "pfmcold").equals(name))
            return Integer.toString(coolantTanks[0].getFill());
        if ((PREFIX_VALUE + "pfm").equals(name)) return Integer.toString(coolantTanks[1].getFill());
        return null;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuPARFC(containerId, playerInventory, this);
    }
}
