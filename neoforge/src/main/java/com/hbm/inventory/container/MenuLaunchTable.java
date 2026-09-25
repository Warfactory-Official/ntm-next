// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.item.IDesignatorItem;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.ModItems;
import com.hbm.items.machine.IFluidContainerItem;
import com.hbm.items.machine.ItemFluidContainerInfinite;
import com.hbm.items.weapon.ItemCustomMissile;
import com.hbm.tileentity.bomb.BlockEntityLaunchTable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MenuLaunchTable extends BlockEntityMenu<BlockEntityLaunchTable> {

    private static final int SLOTS = BlockEntityLaunchTable.SLOT_COUNT;

    public MenuLaunchTable(int containerId, Inventory playerInv, BlockEntityLaunchTable table) {
        this(containerId, playerInv, table, table);
    }

    private MenuLaunchTable(
            int containerId,
            Inventory playerInv,
            Container container,
            BlockEntityLaunchTable table) {
        super(ModMenus.LAUNCH_TABLE.get(), containerId, table, container);
        checkContainerSize(container, SLOTS);

        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityLaunchTable.SLOT_MISSILE,
                        26,
                        36,
                        stack -> stack.getItem() instanceof ItemCustomMissile));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityLaunchTable.SLOT_DESIGNATOR,
                        26,
                        72,
                        stack -> stack.getItem() instanceof IDesignatorItem));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityLaunchTable.SLOT_FUEL_IN,
                        116,
                        72,
                        MenuLaunchTable::isFluidContainer));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityLaunchTable.SLOT_OXIDIZER_IN,
                        134,
                        72,
                        MenuLaunchTable::isFluidContainer));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityLaunchTable.SLOT_SOLID,
                        152,
                        90,
                        stack -> stack.is(ModItems.ROCKET_FUEL.get())));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityLaunchTable.SLOT_BATTERY,
                        116,
                        108,
                        MenuLaunchTable::isBattery));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityLaunchTable.SLOT_FUEL_OUT,
                        116,
                        90,
                        SlotFiltered.NONE));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityLaunchTable.SLOT_OXIDIZER_OUT,
                        134,
                        90,
                        SlotFiltered.NONE));

        addStandardInventorySlots(playerInv, 8, 140);
    }

    private static boolean isBattery(ItemStack stack) {
        return IBatteryItem.isBattery(stack) || stack.is(ModItems.BATTERY_CREATIVE.get());
    }

    private static boolean isFluidContainer(ItemStack stack) {
        return FluidTankNTM.isFluidContainer(stack);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveUnsorted(player, index, SLOTS);
    }
}
