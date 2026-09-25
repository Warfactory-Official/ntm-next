// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotPattern;
import com.hbm.tileentity.machine.storage.BlockEntityMassStorage;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMassStorage extends BlockEntityMenu<BlockEntityMassStorage> {

    public MenuMassStorage(int containerId, Inventory playerInv, BlockEntityMassStorage storage) {
        this(containerId, playerInv, storage, storage);
    }

    private MenuMassStorage(
            int containerId,
            Inventory playerInv,
            Container container,
            BlockEntityMassStorage storage) {
        super(ModMenus.MASS_STORAGE.get(), containerId, storage, container);
        checkContainerSize(container, BlockEntityMassStorage.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        storage,
                        BlockEntityMassStorage.SLOT_INPUT,
                        61,
                        17,
                        stack -> storage.canPlaceItem(BlockEntityMassStorage.SLOT_INPUT, stack)));
        addSlot(new SlotPattern(container, BlockEntityMassStorage.SLOT_TYPE, 61, 53));
        addSlot(
                new SlotFiltered(
                        storage, BlockEntityMassStorage.SLOT_OUTPUT, 61, 89, SlotFiltered.NONE));

        addStandardInventorySlots(playerInv, 8, 139);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        BlockEntityMassStorage storage = blockEntity();
        int machineEnd = BlockEntityMassStorage.SLOT_COUNT;

        if (index == BlockEntityMassStorage.SLOT_OUTPUT && !getSlot(index).hasItem()) {
            getSlot(index).set(storage.quickExtract());
        }

        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (storage.quickInsert(stack)) return true;
                    return moveItemStackTo(
                            stack,
                            BlockEntityMassStorage.SLOT_INPUT,
                            BlockEntityMassStorage.SLOT_INPUT + 1,
                            false);
                });
    }

    @Override
    public void clicked(
            int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        if (slotIndex != BlockEntityMassStorage.SLOT_TYPE) {
            super.clicked(slotIndex, buttonNum, containerInput, player);
            return;
        }
        if (blockEntity().getStockpile() > 0 || !blockEntity().acceptsFilter(getCarried())) return;
        getSlot(slotIndex).set(getCarried());
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.index != BlockEntityMassStorage.SLOT_TYPE
                && super.canTakeItemForPickAll(stack, slot);
    }
}
