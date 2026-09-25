// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.api.block.IDroneContainer;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuDroneProvider;
import com.hbm.tileentity.network.RequestNetwork.OfferNode;
import com.hbm.tileentity.network.RequestNetwork.PathNode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityDroneProvider extends BlockEntityRequestNetwork
        implements MenuProvider, IDroneContainer.Provider {

    public static final int SLOT_COUNT = 9;

    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};

    public BlockEntityDroneProvider(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRONE_PROVIDER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    protected PathNode createNode(BlockPos pos) {
        List<ItemStack> offer = new ArrayList<>();
        for (ItemStack stack : inventory) if (!stack.isEmpty()) offer.add(stack.copy());
        return new OfferNode(pos, reachable, offer);
    }

    @Override
    public Container droneOfferInventory() {
        return this;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.droneProvider");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuDroneProvider(containerId, playerInventory, this);
    }
}
