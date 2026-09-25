// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityMachineCrystallizer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineCrystallizer extends BlockEntityMenu<BlockEntityMachineCrystallizer> {

    private final SyncedData data;

    public MenuMachineCrystallizer(
            int containerId, Inventory playerInv, BlockEntityMachineCrystallizer be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityMachineCrystallizer.class)
                        : SyncedData.of(be),
                be);
    }

    private MenuMachineCrystallizer(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            BlockEntityMachineCrystallizer be) {
        super(ModMenus.MACHINE_CRYSTALLIZER.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineCrystallizer.SLOT_COUNT);
        this.data = data;

        addSlot(new Slot(container, BlockEntityMachineCrystallizer.SLOT_INPUT, 62, 45));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineCrystallizer.SLOT_BATTERY,
                        152,
                        72,
                        IBatteryItem::isBattery));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        container,
                        BlockEntityMachineCrystallizer.SLOT_OUTPUT,
                        113,
                        45));
        addSlot(new Slot(container, BlockEntityMachineCrystallizer.SLOT_FLUID_IN, 17, 18));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineCrystallizer.SLOT_FLUID_OUT,
                        17,
                        54,
                        SlotFiltered.NONE));
        addSlot(
                new SlotUpgrade(
                        container, BlockEntityMachineCrystallizer.SLOT_UPGRADE_START, 80, 18));
        addSlot(
                new SlotUpgrade(
                        container, BlockEntityMachineCrystallizer.SLOT_UPGRADE_END, 98, 18));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineCrystallizer.SLOT_FLUID_ID,
                        35,
                        72,
                        stack -> stack.getItem() instanceof FluidIdentifierItem));

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
                true,
                stack -> {
                    int machineEnd = BlockEntityMachineCrystallizer.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineCrystallizer.SLOT_BATTERY,
                                BlockEntityMachineCrystallizer.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineCrystallizer.SLOT_FLUID_ID,
                                BlockEntityMachineCrystallizer.SLOT_FLUID_ID + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineCrystallizer.SLOT_UPGRADE_START,
                                BlockEntityMachineCrystallizer.SLOT_UPGRADE_END + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityMachineCrystallizer.SLOT_INPUT,
                            BlockEntityMachineCrystallizer.SLOT_INPUT + 1,
                            false);
                });
    }
}
