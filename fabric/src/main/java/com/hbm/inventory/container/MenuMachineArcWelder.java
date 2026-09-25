// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityMachineArcWelder;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineArcWelder extends BlockEntityMenu<BlockEntityMachineArcWelder> {

    private final SyncedData data;

    public MenuMachineArcWelder(
            int containerId, Inventory playerInv, BlockEntityMachineArcWelder be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityMachineArcWelder.class)
                        : SyncedData.of(be),
                be);
    }

    private MenuMachineArcWelder(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            BlockEntityMachineArcWelder be) {
        super(ModMenus.MACHINE_ARC_WELDER.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineArcWelder.SLOT_COUNT);
        this.data = data;

        addSlot(new Slot(container, 0, 17, 36));
        addSlot(new Slot(container, 1, 35, 36));
        addSlot(new Slot(container, 2, 53, 36));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        container,
                        BlockEntityMachineArcWelder.SLOT_OUTPUT,
                        107,
                        36));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineArcWelder.SLOT_BATTERY,
                        152,
                        72,
                        IBatteryItem::isBattery));
        addSlot(new Slot(container, BlockEntityMachineArcWelder.SLOT_FLUID_ID, 17, 63));
        addSlot(new SlotUpgrade(container, BlockEntityMachineArcWelder.SLOT_UPGRADE_START, 89, 63));
        addSlot(new SlotUpgrade(container, BlockEntityMachineArcWelder.SLOT_UPGRADE_END, 107, 63));

        addStandardInventorySlots(playerInv, 8, 122);
        addDataSlots(data);
    }

    public int getProgress() {
        return data.getInt("progress");
    }

    public int getProcessTime() {
        return data.getInt("processTime");
    }

    public int getProgressScaled(int i) {
        int max = getProcessTime();
        return max <= 0 ? 0 : Math.min(i, getProgress() * i / max);
    }

    public long getPower() {
        return data.get("power");
    }

    public long getMaxPower() {
        return data.get("maxPower");
    }

    public long getConsumption() {
        return data.get("consumption");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityMachineArcWelder.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineArcWelder.SLOT_BATTERY,
                                BlockEntityMachineArcWelder.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineArcWelder.SLOT_FLUID_ID,
                                BlockEntityMachineArcWelder.SLOT_FLUID_ID + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineArcWelder.SLOT_UPGRADE_START,
                                BlockEntityMachineArcWelder.SLOT_UPGRADE_END + 1,
                                false);

                    return moveItemStackTo(stack, 0, 3, false);
                });
    }
}
