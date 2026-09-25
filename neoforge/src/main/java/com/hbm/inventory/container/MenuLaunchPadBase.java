// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.item.IDesignatorItem;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFluidContainerInfinite;
import com.hbm.tileentity.bomb.BlockEntityLaunchPadBase;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

public abstract class MenuLaunchPadBase<T extends BlockEntityLaunchPadBase>
        extends BlockEntityMenu<T> {

    private static final int SLOTS = BlockEntityLaunchPadBase.SLOT_COUNT;

    private final T pad;

    protected MenuLaunchPadBase(
            MenuType<?> type, int containerId, Inventory playerInv, Container container, T pad) {
        super(type, containerId, pad, container);
        checkContainerSize(container, SLOTS);
        this.pad = pad;

        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityLaunchPadBase.SLOT_MISSILE,
                        26,
                        36,
                        pad::isMissileValid));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityLaunchPadBase.SLOT_DESIGNATOR,
                        26,
                        72,
                        stack -> stack.getItem() instanceof IDesignatorItem));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityLaunchPadBase.SLOT_BATTERY,
                        107,
                        90,
                        MenuLaunchPadBase::isBattery));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityLaunchPadBase.SLOT_FUEL_IN,
                        125,
                        90,
                        MenuLaunchPadBase::isFluidContainer));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityLaunchPadBase.SLOT_FUEL_OUT,
                        125,
                        108,
                        SlotFiltered.NONE));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityLaunchPadBase.SLOT_OXIDIZER_IN,
                        143,
                        90,
                        MenuLaunchPadBase::isFluidContainer));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityLaunchPadBase.SLOT_OXIDIZER_OUT,
                        143,
                        108,
                        SlotFiltered.NONE));

        addStandardInventorySlots(playerInv, 8, 154);
    }

    private static boolean isBattery(ItemStack stack) {
        return IBatteryItem.isBattery(stack) || stack.is(ModItems.BATTERY_CREATIVE.get());
    }

    private static boolean isFluidContainer(ItemStack stack) {
        return FluidTankNTM.isFluidContainer(stack);
    }

    private static boolean holds(ItemStack stack, FluidTankNTM tank) {
        Fluid type = tank.getTankType();
        if (type == null) return false;
        if (stack.getItem() instanceof ItemFluidContainerInfinite infinite)
            return infinite.fluid() == type;
        return tank.containerContent(stack) > 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int invEnd = slots.size();

                    if (index < SLOTS) return moveItemStackTo(stack, SLOTS, invEnd, true);

                    if (isBattery(stack)) {
                        return moveItemStackTo(
                                stack,
                                BlockEntityLaunchPadBase.SLOT_BATTERY,
                                BlockEntityLaunchPadBase.SLOT_BATTERY + 1,
                                false);
                    }
                    if (pad.isMissileValid(stack)) {
                        return moveItemStackTo(
                                stack,
                                BlockEntityLaunchPadBase.SLOT_MISSILE,
                                BlockEntityLaunchPadBase.SLOT_MISSILE + 1,
                                false);
                    }
                    if (stack.is(ModItems.FLUID_BARREL_INFINITE.get())) {
                        return moveItemStackTo(
                                        stack,
                                        BlockEntityLaunchPadBase.SLOT_FUEL_IN,
                                        BlockEntityLaunchPadBase.SLOT_FUEL_IN + 1,
                                        false)
                                || moveItemStackTo(
                                        stack,
                                        BlockEntityLaunchPadBase.SLOT_OXIDIZER_IN,
                                        BlockEntityLaunchPadBase.SLOT_OXIDIZER_IN + 1,
                                        false);
                    }
                    if (holds(stack, pad.fuelTank)) {
                        return moveItemStackTo(
                                stack,
                                BlockEntityLaunchPadBase.SLOT_FUEL_IN,
                                BlockEntityLaunchPadBase.SLOT_FUEL_IN + 1,
                                false);
                    }
                    if (holds(stack, pad.oxidizerTank)) {
                        return moveItemStackTo(
                                stack,
                                BlockEntityLaunchPadBase.SLOT_OXIDIZER_IN,
                                BlockEntityLaunchPadBase.SLOT_OXIDIZER_IN + 1,
                                false);
                    }
                    if (stack.getItem() instanceof IDesignatorItem) {
                        return moveItemStackTo(
                                stack,
                                BlockEntityLaunchPadBase.SLOT_DESIGNATOR,
                                BlockEntityLaunchPadBase.SLOT_DESIGNATOR + 1,
                                false);
                    }
                    return false;
                });
    }
}
