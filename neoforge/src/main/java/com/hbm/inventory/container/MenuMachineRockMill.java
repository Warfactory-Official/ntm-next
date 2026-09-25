// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.tileentity.machine.BlockEntityMachineRockMill;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachineRockMill extends BlockEntityMenu<BlockEntityMachineRockMill> {
    private final SyncedData data;

    public MenuMachineRockMill(
            int containerId, Inventory playerInventory, BlockEntityMachineRockMill be) {
        this(
                containerId,
                playerInventory,
                be,
                playerInventory.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityMachineRockMill.class)
                        : SyncedData.of(be));
    }

    private MenuMachineRockMill(
            int containerId,
            Inventory playerInventory,
            BlockEntityMachineRockMill be,
            SyncedData data) {
        super(ModMenus.MACHINE_ROCK_MILL.get(), containerId, be, be);
        checkContainerSize(be, BlockEntityMachineRockMill.SLOT_COUNT);
        this.data = data;

        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineRockMill.SLOT_BATTERY,
                        152,
                        91,
                        IBatteryItem::isBattery));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineRockMill.SLOT_SCHEMATIC,
                        35,
                        90,
                        stack -> stack.getItem() instanceof ItemBlueprints));
        for (int i = 0; i < 3; i++) {
            addSlot(
                    SlotFiltered.gated(
                            be, BlockEntityMachineRockMill.SLOT_INPUT_START + i, 8 + i * 18, 27));
        }
        for (int i = 0; i < 3; i++) {
            addSlot(
                    new SlotRecipeOutput(
                            playerInventory.player,
                            be,
                            BlockEntityMachineRockMill.SLOT_OUTPUT_START + i,
                            80 + i * 18,
                            27));
        }
        addStandardInventorySlots(playerInventory, 8, 138);
        addDataSlots(data);
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
                    int machineEnd = BlockEntityMachineRockMill.SLOT_COUNT;
                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (IBatteryItem.isBattery(stack)) return moveItemStackTo(stack, 0, 1, false);
                    if (stack.getItem() instanceof ItemBlueprints)
                        return moveItemStackTo(stack, 1, 2, false);
                    return moveItemStackTo(stack, 2, 5, false);
                });
    }
}
