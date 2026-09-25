// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.inventory.ItemStackContainer;
import com.hbm.inventory.container.MenuItemBox;
import com.hbm.inventory.container.ModMenus;
import com.hbm.lib.Library;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class ItemPlasticBag extends Item {

    public static final MenuItemBox.Layout LAYOUT =
            new MenuItemBox.Layout(
                    1,
                    1,
                    80,
                    65,
                    8,
                    134,
                    192,
                    176,
                    216,
                    6,
                    0x404040,
                    216 - 96 + 2,
                    Library.id("textures/gui/storage/gui_plastic_bag.png"),
                    false);

    public ItemPlasticBag(Properties properties) {
        super(
                properties
                        .stacksTo(1)
                        .component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
    }

    public static ItemStack getContents(ItemStack bag) {
        ItemContainerContents contents = bag.get(DataComponents.CONTAINER);
        return contents == null ? ItemStack.EMPTY : contents.copyOne();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            player.openMenu(
                    new SimpleMenuProvider(
                            (id, inv, opener) ->
                                    new MenuItemBox(
                                            ModMenus.PLASTIC_BAG.get(),
                                            id,
                                            inv,
                                            LAYOUT,
                                            new ItemStackContainer(opener, stack, LAYOUT.slots())),
                            stack.getHoverName()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        ItemStack held = getContents(stack);
        if (!held.isEmpty()) adder.accept(held.getHoverName());
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
