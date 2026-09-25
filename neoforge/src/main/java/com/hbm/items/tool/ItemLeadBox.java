// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.inventory.ItemStackContainer;
import com.hbm.inventory.container.MenuItemBox;
import com.hbm.inventory.container.ModMenus;
import com.hbm.lib.Library;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemLeadBox extends Item {

    public static final MenuItemBox.Layout LAYOUT =
            new MenuItemBox.Layout(
                    4,
                    5,
                    43,
                    18,
                    8,
                    104,
                    162,
                    176,
                    186,
                    6,
                    0x404040,
                    92,
                    Library.id("textures/gui/gui_containment.png"));

    public ItemLeadBox(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            player.openMenu(
                    new SimpleMenuProvider(
                            (id, inv, opener) ->
                                    new MenuItemBox(
                                            ModMenus.CONTAINMENT_BOX.get(),
                                            id,
                                            inv,
                                            LAYOUT,
                                            new ItemStackContainer(opener, stack, LAYOUT.slots())),
                            stack.getHoverName()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
