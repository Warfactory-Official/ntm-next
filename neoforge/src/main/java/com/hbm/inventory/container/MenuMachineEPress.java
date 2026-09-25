// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.ItemStamp;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityMachineEPress;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachineEPress extends BlockEntityMenu<BlockEntityMachineEPress> {

    public MenuMachineEPress(int containerId, Inventory playerInv, BlockEntityMachineEPress be) {
        super(ModMenus.MACHINE_EPRESS.get(), containerId, be);
        checkContainerSize(be, BlockEntityMachineEPress.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineEPress.SLOT_BATTERY,
                        152,
                        54,
                        IBatteryItem::isBattery));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineEPress.SLOT_STAMP,
                        19,
                        15,
                        stack -> stack.getItem() instanceof ItemStamp));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineEPress.SLOT_INPUT,
                        19,
                        51,
                        stack -> !(stack.getItem() instanceof ItemStamp)));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, be, BlockEntityMachineEPress.SLOT_OUTPUT, 79, 33));
        addSlot(new SlotUpgrade(be, BlockEntityMachineEPress.SLOT_UPGRADE, 111, 32));

        addStandardInventorySlots(playerInv, 8, 104);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityMachineEPress.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineEPress.SLOT_BATTERY,
                                BlockEntityMachineEPress.SLOT_BATTERY + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineEPress.SLOT_UPGRADE,
                                BlockEntityMachineEPress.SLOT_UPGRADE + 1,
                                false);
                    if (stack.getItem() instanceof ItemStamp)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineEPress.SLOT_STAMP,
                                BlockEntityMachineEPress.SLOT_STAMP + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityMachineEPress.SLOT_INPUT,
                            BlockEntityMachineEPress.SLOT_INPUT + 1,
                            false);
                });
    }
}
