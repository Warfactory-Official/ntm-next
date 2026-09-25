// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityMachineTurbofan;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachineTurbofan extends BlockEntityMenu<BlockEntityMachineTurbofan> {

    public MenuMachineTurbofan(
            int containerId, Inventory playerInv, BlockEntityMachineTurbofan be) {
        super(ModMenus.MACHINE_TURBOFAN.get(), containerId, be);
        checkContainerSize(be, BlockEntityMachineTurbofan.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineTurbofan.SLOT_FLUID_IN,
                        17,
                        17,
                        stack -> be.canPlaceItem(BlockEntityMachineTurbofan.SLOT_FLUID_IN, stack)));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineTurbofan.SLOT_CONTAINER_OUT,
                        17,
                        53,
                        SlotFiltered.NONE));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineTurbofan.SLOT_UPGRADE,
                        98,
                        71,
                        stack -> be.canPlaceItem(BlockEntityMachineTurbofan.SLOT_UPGRADE, stack)));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineTurbofan.SLOT_BATTERY,
                        143,
                        71,
                        IBatteryItem::isBattery));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineTurbofan.SLOT_FLUID_ID,
                        44,
                        71,
                        stack -> stack.getItem() instanceof FluidIdentifierItem));

        addStandardInventorySlots(playerInv, 8, 121);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityMachineTurbofan.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineTurbofan.SLOT_BATTERY,
                                BlockEntityMachineTurbofan.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineTurbofan.SLOT_FLUID_ID,
                                BlockEntityMachineTurbofan.SLOT_FLUID_ID + 1,
                                false);
                    if (stack.getItem() instanceof ItemMachineUpgrade)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineTurbofan.SLOT_UPGRADE,
                                BlockEntityMachineTurbofan.SLOT_UPGRADE + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityMachineTurbofan.SLOT_FLUID_IN,
                            BlockEntityMachineTurbofan.SLOT_FLUID_IN + 1,
                            false);
                });
    }
}
