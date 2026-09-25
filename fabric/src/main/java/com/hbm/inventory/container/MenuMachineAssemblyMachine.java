// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityMachineAssemblyMachine;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachineAssemblyMachine extends BlockEntityMenu<BlockEntityMachineAssemblyMachine> {

    private final SyncedData data;

    public MenuMachineAssemblyMachine(
            int containerId, Inventory playerInv, BlockEntityMachineAssemblyMachine be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityMachineAssemblyMachine.class)
                        : SyncedData.of(be),
                be);
    }

    private MenuMachineAssemblyMachine(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            BlockEntityMachineAssemblyMachine be) {
        super(ModMenus.MACHINE_ASSEMBLY_MACHINE.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineAssemblyMachine.SLOT_COUNT);
        this.data = data;

        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineAssemblyMachine.SLOT_BATTERY,
                        152,
                        81,
                        IBatteryItem::isBattery));

        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineAssemblyMachine.SLOT_BLUEPRINT,
                        35,
                        126,
                        stack -> stack.getItem() instanceof ItemBlueprints));
        addSlot(
                new SlotUpgrade(
                        container, BlockEntityMachineAssemblyMachine.SLOT_UPGRADE_START, 152, 108));
        addSlot(
                new SlotUpgrade(
                        container,
                        BlockEntityMachineAssemblyMachine.SLOT_UPGRADE_START + 1,
                        170,
                        108));

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 3; col++) {
                int slot = BlockEntityMachineAssemblyMachine.SLOT_INPUT_START + row * 3 + col;
                addSlot(SlotFiltered.gated(container, slot, 8 + col * 18, 18 + row * 18));
            }
        }

        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        container,
                        BlockEntityMachineAssemblyMachine.SLOT_OUTPUT,
                        98,
                        45));

        addStandardInventorySlots(playerInv, 8, 174);
        addDataSlots(data);
    }

    public int getProgressScaled(int i) {
        return (int) Math.ceil(i * blockEntity().recipeModule.progress);
    }

    public long getPower() {
        return data.get("power");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityMachineAssemblyMachine.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineAssemblyMachine.SLOT_BATTERY,
                                BlockEntityMachineAssemblyMachine.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof ItemBlueprints)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineAssemblyMachine.SLOT_BLUEPRINT,
                                BlockEntityMachineAssemblyMachine.SLOT_BLUEPRINT + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineAssemblyMachine.SLOT_UPGRADE_START,
                                BlockEntityMachineAssemblyMachine.SLOT_UPGRADE_END + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityMachineAssemblyMachine.SLOT_INPUT_START,
                            BlockEntityMachineAssemblyMachine.SLOT_INPUT_END + 1,
                            false);
                });
    }
}
