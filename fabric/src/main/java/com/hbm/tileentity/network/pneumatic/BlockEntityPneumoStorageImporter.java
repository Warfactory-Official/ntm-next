// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network.pneumatic;

import com.hbm.api.ntl.PneumaticNetwork;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuPneumoStorageImporter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityPneumoStorageImporter extends BlockEntityPneumaticMachineBase {

    public static final int SLOT_COUNT = 9;
    private static final int[] SLOT_ACCESS = {0, 1, 2, 3, 4, 5, 6, 7, 8};

    public int[] delay = new int[SLOT_COUNT];

    public BlockEntityPneumoStorageImporter(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PNEUMATIC_STORAGE_IMPORTER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void setItem(int i, ItemStack stack) {
        super.setItem(i, stack);

        if (!stack.isEmpty()) this.delay[i] = Math.max(this.delay[i], 1);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOT_ACCESS;
    }

    @Override
    public void tickServer() {
        super.tickServer();

        PneumaticNetwork net = net();
        if (net != null)
            for (int i = 0; i < SLOT_COUNT; i++) {
                if (this.delay[i] > 0) {
                    this.delay[i]--;
                    continue;
                }
                ItemStack stack = inventory.get(i);
                if (stack.isEmpty()) continue;

                int stored =
                        (int) net.give((ServerLevel) level, worldPosition, stack, stack.getCount());
                if (stored <= 0) {
                    this.delay[i] = 100;
                } else {
                    this.removeItem(i, stored);
                }
            }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.pneumoStorageImporter");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuPneumoStorageImporter(containerId, playerInventory, this);
    }
}
