// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.ItemDrillbit;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityMachineExcavator;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineExcavator extends BlockEntityMenu<BlockEntityMachineExcavator> {

    private final SyncedData data;

    public MenuMachineExcavator(
            int containerId, Inventory playerInv, BlockEntityMachineExcavator be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityMachineExcavator.class)
                        : SyncedData.of(be),
                be);
    }

    private MenuMachineExcavator(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            BlockEntityMachineExcavator be) {
        super(ModMenus.MACHINE_EXCAVATOR.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineExcavator.SLOT_COUNT);
        this.data = data;

        addSlot(new Slot(container, BlockEntityMachineExcavator.SLOT_BATTERY, 220, 72));
        addSlot(new Slot(container, BlockEntityMachineExcavator.SLOT_FLUID_ID, 202, 72));
        for (int i = 0; i < 3; i++) {
            addSlot(
                    new Slot(
                            container,
                            BlockEntityMachineExcavator.SLOT_UPGRADE_START + i,
                            136 + i * 18,
                            75));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(
                        new SlotFiltered(
                                container,
                                BlockEntityMachineExcavator.SLOT_BUFFER_START + col + row * 3,
                                136 + col * 18,
                                5 + row * 18,
                                SlotFiltered.NONE));
            }
        }

        addStandardInventorySlots(playerInv, 41, 122);
        addDataSlots(data);
    }

    public long getPower() {
        return data.get("power");
    }

    public int getPowerScaled(int i) {
        return (int) (getPower() * i / BlockEntityMachineExcavator.MAX_POWER);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityMachineExcavator.SLOT_COUNT;
                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineExcavator.SLOT_BATTERY,
                                BlockEntityMachineExcavator.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineExcavator.SLOT_FLUID_ID,
                                BlockEntityMachineExcavator.SLOT_FLUID_ID + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineExcavator.SLOT_UPGRADE_START,
                                BlockEntityMachineExcavator.SLOT_UPGRADE_END + 1,
                                false);
                    if (stack.getItem() instanceof ItemDrillbit)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineExcavator.SLOT_DRILL,
                                BlockEntityMachineExcavator.SLOT_DRILL + 1,
                                false);

                    return false;
                });
    }
}
