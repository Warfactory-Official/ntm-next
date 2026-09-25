// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionTorus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuFusionTorus extends BlockEntityMenu<BlockEntityFusionTorus> {

    public MenuFusionTorus(int containerId, Inventory playerInv, BlockEntityFusionTorus be) {
        super(ModMenus.FUSION_TORUS.get(), containerId, be);
        checkContainerSize(be, BlockEntityFusionTorus.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        be, BlockEntityFusionTorus.SLOT_BATTERY, 8, 82, IBatteryItem::isBattery));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityFusionTorus.SLOT_BLUEPRINT,
                        71,
                        81,
                        stack -> stack.getItem() instanceof ItemBlueprints));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, be, BlockEntityFusionTorus.SLOT_OUTPUT, 130, 36));

        addStandardInventorySlots(playerInv, 35, 162);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityFusionTorus.SLOT_COUNT;
                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityFusionTorus.SLOT_BATTERY,
                                BlockEntityFusionTorus.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof ItemBlueprints)
                        return moveItemStackTo(
                                stack,
                                BlockEntityFusionTorus.SLOT_BLUEPRINT,
                                BlockEntityFusionTorus.SLOT_BLUEPRINT + 1,
                                false);
                    return false;
                });
    }
}
