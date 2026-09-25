// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.tileentity.machine.BlockEntityMachineSuperComputer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachineSuperComputer extends BlockEntityMenu<BlockEntityMachineSuperComputer> {
    private final SyncedData data;

    public MenuMachineSuperComputer(
            int containerId, Inventory playerInventory, BlockEntityMachineSuperComputer be) {
        this(
                containerId,
                playerInventory,
                be,
                playerInventory.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityMachineSuperComputer.class)
                        : SyncedData.of(be));
    }

    private MenuMachineSuperComputer(
            int containerId,
            Inventory playerInventory,
            BlockEntityMachineSuperComputer be,
            SyncedData data) {
        super(ModMenus.MACHINE_SUPERCOMPUTER.get(), containerId, be, be);
        checkContainerSize(be, BlockEntityMachineSuperComputer.SLOT_COUNT);
        this.data = data;
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineSuperComputer.SLOT_BATTERY,
                        152,
                        81,
                        IBatteryItem::isBattery));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineSuperComputer.SLOT_SCHEMATIC,
                        35,
                        80,
                        stack -> stack.getItem() instanceof ItemBlueprints));
        for (int i = 0; i < 3; i++) {
            addSlot(
                    SlotFiltered.gated(
                            be,
                            BlockEntityMachineSuperComputer.SLOT_INPUT_START + i,
                            8 + i * 18,
                            27));
        }
        for (int i = 0; i < 3; i++) {
            addSlot(
                    new SlotRecipeOutput(
                            playerInventory.player,
                            be,
                            BlockEntityMachineSuperComputer.SLOT_OUTPUT_START + i,
                            80 + i * 18,
                            27));
        }
        addStandardInventorySlots(playerInventory, 8, 129);
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
                    if (index < BlockEntityMachineSuperComputer.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineSuperComputer.SLOT_COUNT,
                                slots.size(),
                                true);
                    }
                    if (IBatteryItem.isBattery(stack)) return moveItemStackTo(stack, 0, 1, false);
                    if (stack.getItem() instanceof ItemBlueprints)
                        return moveItemStackTo(stack, 1, 2, false);
                    return moveItemStackTo(stack, 2, 5, false);
                });
    }
}
