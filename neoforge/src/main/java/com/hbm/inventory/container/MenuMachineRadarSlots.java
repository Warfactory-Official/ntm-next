// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemRadarLinker;
import com.hbm.tileentity.machine.BlockEntityMachineRadar;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineRadarSlots extends BlockEntityMenu<BlockEntityMachineRadar> {

    private static final int MACHINE_SLOTS = BlockEntityMachineRadar.SLOT_COUNT;

    public MenuMachineRadarSlots(int containerId, Inventory playerInv, BlockEntityMachineRadar be) {
        super(ModMenus.MACHINE_RADAR_SLOTS.get(), containerId, be);

        for (int i = 0; i < BlockEntityMachineRadar.SLOT_LINK_COUNT; i++) {
            addSlot(new Slot(be, i, 26 + i * 18, 17));
        }

        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineRadar.SLOT_LINKER,
                        26,
                        44,
                        stack -> stack.getItem() instanceof ItemRadarLinker));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineRadar.SLOT_BATTERY,
                        152,
                        44,
                        MenuMachineRadarSlots::isBattery));

        addStandardInventorySlots(playerInv, 8, 103);
    }

    private static boolean isBattery(ItemStack stack) {
        return IBatteryItem.isBattery(stack) || stack.is(ModItems.BATTERY_CREATIVE.get());
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity().isRadarMenuValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    if (index < MACHINE_SLOTS)
                        return moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true);
                    if (isBattery(stack)) {
                        return moveItemStackTo(
                                stack, BlockEntityMachineRadar.SLOT_BATTERY, MACHINE_SLOTS, false);
                    }
                    return moveItemStackTo(stack, 0, BlockEntityMachineRadar.SLOT_BATTERY, false);
                });
    }
}
