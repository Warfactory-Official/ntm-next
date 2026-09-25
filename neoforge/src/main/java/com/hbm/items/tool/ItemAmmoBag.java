// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.inventory.ItemStackContainer;
import com.hbm.inventory.container.MenuItemBox;
import com.hbm.inventory.container.ModMenus;
import com.hbm.lib.Library;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemAmmoBag extends Item {

    public static final MenuItemBox.Layout LAYOUT =
            new MenuItemBox.Layout(
                    2,
                    4,
                    53,
                    18,
                    8,
                    82,
                    140,
                    176,
                    168,
                    6,
                    0xFFFFFF,
                    70,
                    Library.id("textures/gui/gui_ammo_bag.png"));

    private static final int EMPTY_SLOT_ROOM = 64;
    private static final int BAR_WIDTH = 13;

    private final boolean infinite;

    public ItemAmmoBag(Properties properties, boolean infinite) {
        super(properties);
        this.infinite = infinite;
    }

    public boolean infinite() {
        return infinite;
    }

    public static ItemStackContainer contents(ItemStack bag) {
        return new ItemStackContainer(null, bag, LAYOUT.slots());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            player.openMenu(
                    new SimpleMenuProvider(
                            (id, inv, opener) ->
                                    new MenuItemBox(
                                            ModMenus.AMMO_BAG.get(),
                                            id,
                                            inv,
                                            LAYOUT,
                                            new ItemStackContainer(opener, stack, LAYOUT.slots())),
                            stack.getHoverName()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !infinite && fill(stack) < 1F;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(BAR_WIDTH * fill(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(fill(stack) / 3.0F, 1.0F, 1.0F);
    }

    private static float fill(ItemStack bag) {
        ItemStackContainer contents = contents(bag);
        int room = 0;
        int rounds = 0;
        for (int slot = 0; slot < contents.getContainerSize(); slot++) {
            ItemStack held = contents.getItem(slot);
            if (held.isEmpty()) {
                room += EMPTY_SLOT_ROOM;
            } else {
                room += held.getMaxStackSize();
                rounds += held.getCount();
            }
        }
        return room == 0 ? 0F : (float) rounds / (float) room;
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
