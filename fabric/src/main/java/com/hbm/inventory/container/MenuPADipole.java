// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.ItemPACoil;
import com.hbm.tileentity.machine.albion.BlockEntityPADipole;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuPADipole extends BlockEntityMenu<BlockEntityPADipole> {

    public MenuPADipole(int containerId, Inventory playerInv, BlockEntityPADipole be) {
        super(ModMenus.PA_DIPOLE.get(), containerId, be);
        checkContainerSize(be, BlockEntityPADipole.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        container(),
                        BlockEntityPADipole.SLOT_BATTERY,
                        8,
                        72,
                        IBatteryItem::isBattery));
        addSlot(
                new SlotFiltered(
                        container(),
                        BlockEntityPADipole.SLOT_COIL,
                        89,
                        26,
                        stack -> stack.getItem() instanceof ItemPACoil));

        addStandardInventorySlots(playerInv, 8, 122);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityPADipole.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityPADipole.SLOT_BATTERY,
                                BlockEntityPADipole.SLOT_BATTERY + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityPADipole.SLOT_COIL,
                            BlockEntityPADipole.SLOT_COIL + 1,
                            false);
                });
    }
}
