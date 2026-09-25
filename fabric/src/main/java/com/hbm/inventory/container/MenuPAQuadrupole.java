// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.ItemPACoil;
import com.hbm.tileentity.machine.albion.BlockEntityPAQuadrupole;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuPAQuadrupole extends BlockEntityMenu<BlockEntityPAQuadrupole> {

    public MenuPAQuadrupole(int containerId, Inventory playerInv, BlockEntityPAQuadrupole be) {
        super(ModMenus.PA_QUADRUPOLE.get(), containerId, be);
        checkContainerSize(be, BlockEntityPAQuadrupole.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        container(),
                        BlockEntityPAQuadrupole.SLOT_BATTERY,
                        26,
                        72,
                        IBatteryItem::isBattery));
        addSlot(
                new SlotFiltered(
                        container(),
                        BlockEntityPAQuadrupole.SLOT_COIL,
                        71,
                        36,
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
                    int machineEnd = BlockEntityPAQuadrupole.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityPAQuadrupole.SLOT_BATTERY,
                                BlockEntityPAQuadrupole.SLOT_BATTERY + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityPAQuadrupole.SLOT_COIL,
                            BlockEntityPAQuadrupole.SLOT_COIL + 1,
                            false);
                });
    }
}
