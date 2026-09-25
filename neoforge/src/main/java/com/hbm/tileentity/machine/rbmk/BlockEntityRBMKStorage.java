// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuRBMKStorage;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.util.TickPhase;
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

public class BlockEntityRBMKStorage extends BlockEntityRBMKBase
        implements IRBMKLoadable, MenuProvider, SyncUnitSchema {

    public BlockEntityRBMKStorage(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_STORAGE.get(), pos, state, 12);
    }

    @Override
    public void tickServer() {

        if (TickPhase.every(this, 10)) {
            boolean moved = false;
            for (int i = 0; i < inventory.size() - 1; i++) {
                if (inventory.get(i).isEmpty() && !inventory.get(i + 1).isEmpty()) {
                    inventory.set(i, inventory.get(i + 1));
                    inventory.set(i + 1, ItemStack.EMPTY);
                    moved = true;
                }
            }
            if (moved) markChanged();
        }
        super.tickServer();
    }

    @Override
    public RBMKColumnType getConsoleType() {
        return RBMKColumnType.STORAGE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof ItemRBMKRod;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public boolean canLoad(ItemStack toLoad) {
        return inventory.get(11).isEmpty();
    }

    @Override
    public void load(ItemStack toLoad) {
        inventory.set(11, toLoad.copy());
        markChanged();
    }

    @Override
    public boolean canUnload() {
        return !inventory.get(0).isEmpty();
    }

    @Override
    public ItemStack provideNext() {
        return inventory.get(0);
    }

    @Override
    public void unload() {
        inventory.set(0, ItemStack.EMPTY);
        markChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkStorage");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MenuRBMKStorage(id, inv, this);
    }
}
