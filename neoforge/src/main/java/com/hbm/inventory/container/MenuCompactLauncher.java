// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.item.IDesignatorItem;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemCustomMissile;
import com.hbm.tileentity.bomb.BlockEntityCompactLauncher;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuCompactLauncher extends BlockEntityMenu<BlockEntityCompactLauncher> {

    private static final int SLOTS = BlockEntityCompactLauncher.SLOT_COUNT;

    public MenuCompactLauncher(
            int containerId, Inventory playerInv, BlockEntityCompactLauncher launcher) {
        this(containerId, playerInv, launcher, launcher);
    }

    private MenuCompactLauncher(
            int containerId,
            Inventory playerInv,
            Container container,
            BlockEntityCompactLauncher launcher) {
        super(ModMenus.COMPACT_LAUNCHER.get(), containerId, launcher, container);
        checkContainerSize(container, SLOTS);

        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityCompactLauncher.SLOT_MISSILE,
                        26,
                        36,
                        stack -> stack.getItem() instanceof ItemCustomMissile));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityCompactLauncher.SLOT_DESIGNATOR,
                        26,
                        72,
                        stack -> stack.getItem() instanceof IDesignatorItem));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityCompactLauncher.SLOT_FUEL_IN,
                        116,
                        72,
                        MenuCompactLauncher::isFluidContainer));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityCompactLauncher.SLOT_OXIDIZER_IN,
                        134,
                        72,
                        MenuCompactLauncher::isFluidContainer));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityCompactLauncher.SLOT_SOLID,
                        152,
                        90,
                        stack -> stack.is(ModItems.ROCKET_FUEL.get())));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityCompactLauncher.SLOT_BATTERY,
                        116,
                        108,
                        MenuCompactLauncher::isBattery));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityCompactLauncher.SLOT_FUEL_OUT,
                        116,
                        90,
                        SlotFiltered.NONE));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityCompactLauncher.SLOT_OXIDIZER_OUT,
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
