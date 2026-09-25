// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionPlasmaForge;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachinePlasmaForge extends BlockEntityMenu<BlockEntityFusionPlasmaForge> {

    public MenuMachinePlasmaForge(
            int containerId, Inventory playerInv, BlockEntityFusionPlasmaForge be) {
        super(ModMenus.MACHINE_PLASMA_FORGE.get(), containerId, be);
        checkContainerSize(be, BlockEntityFusionPlasmaForge.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityFusionPlasmaForge.SLOT_BATTERY,
                        152,
                        82,
                        IBatteryItem::isBattery));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityFusionPlasmaForge.SLOT_BLUEPRINT,
                        35,
                        81,
                        stack -> stack.getItem() instanceof ItemBlueprints));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityFusionPlasmaForge.SLOT_BOOSTER,
                        98,
                        116,
                        stack ->
                                be.canPlaceItem(BlockEntityFusionPlasmaForge.SLOT_BOOSTER, stack)));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                int index = BlockEntityFusionPlasmaForge.SLOT_INPUT_START + col + row * 4;
                addSlot(
                        new SlotFiltered(
                                be,
                                index,
                                8 + col * 18,
                                18 + row * 18,
                                stack -> be.canPlaceItem(index, stack)));
            }
        }

        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, be, BlockEntityFusionPlasmaForge.SLOT_OUTPUT, 116, 36));

        addStandardInventorySlots(playerInv, 8, 162);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityFusionPlasmaForge.SLOT_COUNT;

                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityFusionPlasmaForge.SLOT_BATTERY,
                                BlockEntityFusionPlasmaForge.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof ItemBlueprints)
                        return moveItemStackTo(
                                stack,
                                BlockEntityFusionPlasmaForge.SLOT_BLUEPRINT,
                                BlockEntityFusionPlasmaForge.SLOT_BLUEPRINT + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityFusionPlasmaForge.SLOT_BOOSTER,
                            BlockEntityFusionPlasmaForge.SLOT_OUTPUT,
                            false);
                });
    }
}
