/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.items.tool;

import com.hbm.inventory.ItemStackContainer;
import com.hbm.inventory.container.ItemBoxMenu;
import com.hbm.inventory.container.ModMenus;
import com.hbm.util.RegistryUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class AmmoBagItem extends Item {
    public static final ItemBoxMenu.Layout LAYOUT =
            new ItemBoxMenu.Layout(
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
                    RegistryUtil.id("textures/gui/gui_ammo_bag.png"));
    private static final int EMPTY_SLOT_ROOM = 64;
    private static final int BAR_WIDTH = 13;

    private final boolean infinite;

    public AmmoBagItem(Properties properties, boolean infinite) {
        super(properties);
        this.infinite = infinite;
    }

    public boolean infinite() {
        return infinite;
    }

    public static ItemStackContainer contents(ItemStack bag) {
        return new ItemStackContainer(null, bag, LAYOUT.slots());
    }

    public static @Nullable ItemStackContainer heldContents(ItemStack slot) {
        return slot.getItem() instanceof AmmoBagItem ? contents(slot) : null;
    }

    public static boolean isInfinite(ItemStack slot) {
        return slot.getItem() instanceof AmmoBagItem bag && bag.infinite;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            player.openMenu(
                    new SimpleMenuProvider(
                            (id, inv, opener) ->
                                    new ItemBoxMenu(
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
