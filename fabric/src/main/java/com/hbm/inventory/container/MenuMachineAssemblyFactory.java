// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityMachineAssemblyFactory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachineAssemblyFactory extends BlockEntityMenu<BlockEntityMachineAssemblyFactory> {

    private static final int MODULES = BlockEntityMachineAssemblyFactory.MODULES;
    private static final int SLOTS_PER_MODULE = BlockEntityMachineAssemblyFactory.SLOTS_PER_MODULE;
    private static final int TEMPLATE_OFFSET = BlockEntityMachineAssemblyFactory.TEMPLATE_OFFSET;
    private static final int INPUT_OFFSET = BlockEntityMachineAssemblyFactory.INPUT_OFFSET;
    private static final int OUTPUT_OFFSET = BlockEntityMachineAssemblyFactory.OUTPUT_OFFSET;

    public MenuMachineAssemblyFactory(
            int containerId, Inventory playerInv, BlockEntityMachineAssemblyFactory be) {
        super(ModMenus.MACHINE_ASSEMBLY_FACTORY.get(), containerId, be);
        checkContainerSize(be, BlockEntityMachineAssemblyFactory.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        container(),
                        BlockEntityMachineAssemblyFactory.SLOT_BATTERY,
                        234,
                        112,
                        IBatteryItem::isBattery));
        for (int k = 0; k < 3; k++) {
            addSlot(
                    new SlotUpgrade(
                            container(),
                            BlockEntityMachineAssemblyFactory.SLOT_UPGRADE_START + k,
                            214,
                            149 + k * 18));
        }

        for (int i = 0; i < MODULES; i++) {
            int base = i * SLOTS_PER_MODULE;
            int colX = (i % 2) * 109;
            int rowY = (i / 2) * 56;

            addSlot(
                    new SlotFiltered(
                            container(),
                            base + TEMPLATE_OFFSET,
                            25 + colX,
                            54 + rowY,
                            stack -> stack.getItem() instanceof ItemBlueprints));

            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 6; col++) {
                    int slot = base + INPUT_OFFSET + col + row * 6;
                    addSlot(
                            SlotFiltered.gated(
                                    container(), slot, 7 + colX + col * 16, 20 + rowY + row * 16));
                }
            }

            addSlot(
                    new SlotRecipeOutput(
                            playerInv.player,
                            container(),
                            base + OUTPUT_OFFSET,
                            87 + colX,
                            54 + rowY));
        }

        addStandardInventorySlots(playerInv, 33, 158);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityMachineAssemblyFactory.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineAssemblyFactory.SLOT_BATTERY,
                                BlockEntityMachineAssemblyFactory.SLOT_BATTERY + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineAssemblyFactory.SLOT_UPGRADE_START,
                                BlockEntityMachineAssemblyFactory.SLOT_UPGRADE_END + 1,
                                false);
                    if (stack.getItem() instanceof ItemBlueprints)
                        return moveToAnyModule(stack, TEMPLATE_OFFSET);
                    return moveToAnyModuleInputs(stack);
                });
    }

    private boolean moveToAnyModule(ItemStack stack, int moduleOffset) {
        for (int i = 0; i < MODULES; i++) {
            int slot = i * SLOTS_PER_MODULE + moduleOffset;
            if (moveItemStackTo(stack, slot, slot + 1, false)) return true;
        }
        return false;
    }

    private boolean moveToAnyModuleInputs(ItemStack stack) {
        for (int i = 0; i < MODULES; i++) {
            int base = i * SLOTS_PER_MODULE;
            if (moveItemStackTo(stack, base + INPUT_OFFSET, base + OUTPUT_OFFSET, false))
                return true;
        }
        return false;
    }
}
