// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network.pneumatic;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuPneumoStorageAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityPneumoStorageAccess extends BlockEntityPneumaticMachineBase {

    public BlockEntityPneumoStorageAccess(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PNEUMATIC_STORAGE_ACCESS.get(), pos, state, 0);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.pneumoStorageAccess");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuPneumoStorageAccess(containerId, playerInventory, this);
    }
}
