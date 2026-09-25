// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuSatDock;
import com.hbm.items.ISatChip;
import com.hbm.items.ModItems;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.util.TickPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityMachineSatDock extends BlockEntityMachineBase implements MenuProvider {

    public static final int CARGO_SLOTS = 15;
    public static final int SLOT_CHIP = 15;
    public static final int SLOT_COUNT = 16;
    public static final int CARGO_REQUEST_PERIOD = 20;
    private static final int[] ACCESSIBLE_SLOTS = accessibleSlots();

    public BlockEntityMachineSatDock(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINER_DOCK.get(), pos, state, SLOT_COUNT);
    }

    private static int[] accessibleSlots() {
        int[] slots = new int[CARGO_SLOTS];
        for (int i = 0; i < CARGO_SLOTS; i++) slots[i] = i;
        return slots;
    }

    @Override
    public void tickServer() {
        if (!TickPhase.every(this, CARGO_REQUEST_PERIOD)
                || !getItem(SLOT_CHIP).is(ModItems.SAT_CHIP.get())) return;
        ServerLevel server = (ServerLevel) level;
        SatelliteSavedData data = SatelliteSavedData.get(server);
        Satellite satellite = data.getSatFromFreq(ISatChip.getFreqS(getItem(SLOT_CHIP)));
        if (satellite == null) return;
        BlockPos pos = getBlockPos();
        satellite.tryRequestItems(server, pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_CHIP;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.satDock");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuSatDock(containerId, inventory, this);
    }
}
