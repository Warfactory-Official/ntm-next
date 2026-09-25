// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityMachineMixer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineMixer extends BlockEntityMenu<BlockEntityMachineMixer> {

    private final SyncedData data;

    public MenuMachineMixer(int containerId, Inventory playerInv, BlockEntityMachineMixer be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityMachineMixer.class)
                        : SyncedData.of(be),
                be);
    }

    private MenuMachineMixer(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            BlockEntityMachineMixer be) {
        super(ModMenus.MACHINE_MIXER.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineMixer.SLOT_COUNT);
        this.data = data;

        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineMixer.SLOT_BATTERY,
                        12,
                        72,
                        IBatteryItem::isBattery));
        addSlot(new Slot(container, BlockEntityMachineMixer.SLOT_ITEM_INPUT, 52, 72));
        addSlot(new Slot(container, BlockEntityMachineMixer.SLOT_FLUID_ID, 126, 72));
        addSlot(new SlotUpgrade(container, BlockEntityMachineMixer.SLOT_UPGRADE_START, 148, 18));
        addSlot(
                new SlotUpgrade(
                        container, BlockEntityMachineMixer.SLOT_UPGRADE_START + 1, 148, 36));

        addStandardInventorySlots(playerInv, 8, 122);
        addDataSlots(data);
    }

    public long getPower() {
        return data.get("power");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityMachineMixer.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineMixer.SLOT_BATTERY,
                                BlockEntityMachineMixer.SLOT_BATTERY + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineMixer.SLOT_UPGRADE_START,
                                BlockEntityMachineMixer.SLOT_UPGRADE_END + 1,
                                false);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineMixer.SLOT_FLUID_ID,
                                BlockEntityMachineMixer.SLOT_FLUID_ID + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityMachineMixer.SLOT_ITEM_INPUT,
                            BlockEntityMachineMixer.SLOT_ITEM_INPUT + 1,
                            false);
                });
    }
}
