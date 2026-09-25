// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.tileentity.machine.BlockEntityMachineReactorBreeding;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineReactorBreeding extends BlockEntityMenu<BlockEntityMachineReactorBreeding> {

    private final SyncedData data;

    public MenuMachineReactorBreeding(
            int containerId, Inventory playerInv, BlockEntityMachineReactorBreeding be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityMachineReactorBreeding.class)
                        : SyncedData.of(be),
                be);
    }

    private MenuMachineReactorBreeding(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            BlockEntityMachineReactorBreeding be) {
        super(ModMenus.MACHINE_REACTOR_BREEDING.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineReactorBreeding.SLOT_COUNT);
        this.data = data;

        addSlot(new Slot(container, BlockEntityMachineReactorBreeding.SLOT_INPUT, 35, 35));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        container,
                        BlockEntityMachineReactorBreeding.SLOT_OUTPUT,
                        125,
                        35));

        addStandardInventorySlots(playerInv, 8, 84);
        addDataSlots(data);
    }

    public int getFlux() {
        return (int) data.get("flux");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveUnsorted(player, index, BlockEntityMachineReactorBreeding.SLOT_COUNT);
    }
}
