// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.ItemPistons;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.IFluidContainerItem;
import com.hbm.items.machine.ItemFluidContainerInfinite;
import com.hbm.tileentity.machine.BlockEntityMachineCombustionEngine;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MenuMachineCombustionEngine
        extends BlockEntityMenu<BlockEntityMachineCombustionEngine> {

    private final BlockEntityMachineCombustionEngine engine;

    public MenuMachineCombustionEngine(
            int containerId, Inventory playerInv, BlockEntityMachineCombustionEngine be) {
        super(ModMenus.MACHINE_COMBUSTION_ENGINE.get(), containerId, be);
        checkContainerSize(be, BlockEntityMachineCombustionEngine.SLOT_COUNT);
        this.engine = be;
        be.openInventory();

        addSlot(new Slot(be, BlockEntityMachineCombustionEngine.SLOT_FLUID_IN, 17, 17));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        be,
                        BlockEntityMachineCombustionEngine.SLOT_CONTAINER_OUT,
                        17,
                        53));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineCombustionEngine.SLOT_PISTON,
                        88,
                        71,
                        stack -> stack.getItem() instanceof ItemPistons));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineCombustionEngine.SLOT_BATTERY,
                        143,
                        71,
                        IBatteryItem::isBattery));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineCombustionEngine.SLOT_FLUID_ID,
                        35,
                        71,
                        stack -> stack.getItem() instanceof FluidIdentifierItem));

        addStandardInventorySlots(playerInv, 8, 121);
    }

    private static boolean isFluidContainer(ItemStack stack) {
        return FluidTankNTM.isFluidContainer(stack);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        engine.closeInventory();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityMachineCombustionEngine.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineCombustionEngine.SLOT_BATTERY,
                                BlockEntityMachineCombustionEngine.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineCombustionEngine.SLOT_FLUID_ID,
                                BlockEntityMachineCombustionEngine.SLOT_FLUID_ID + 1,
                                false);
                    if (stack.getItem() instanceof ItemPistons)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineCombustionEngine.SLOT_PISTON,
                                BlockEntityMachineCombustionEngine.SLOT_PISTON + 1,
                                false);
                    if (isFluidContainer(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineCombustionEngine.SLOT_FLUID_IN,
                                BlockEntityMachineCombustionEngine.SLOT_FLUID_IN + 1,
                                false);
                    return false;
                });
    }
}
