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
import com.hbm.items.ModDataComponents;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.util.RegistryUtil;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class CasingBagItem extends Item {
    public static final ItemBoxMenu.Layout LAYOUT =
            new ItemBoxMenu.Layout(
                    3,
                    5,
                    44,
                    18,
                    8,
                    100,
                    158,
                    176,
                    186,
                    6,
                    0xFFFFFF,
                    88,
                    RegistryUtil.id("textures/gui/gui_casing_bag.png"));

    public CasingBagItem(Properties properties) {
        super(properties);
    }

    public static boolean pushCasing(ItemStack bag, ItemStack casing, float amount) {
        if (casing.isEmpty()) return false;
        Map<String, Float> progress =
                new HashMap<>(bag.getOrDefault(ModDataComponents.CASING_PROGRESS.get(), Map.of()));
        String key = key(casing);
        float carried = progress.getOrDefault(key, 0F);
        boolean took = false;

        if (carried < 1F) {
            carried += amount;
            took = true;
        }

        if (carried >= 1F) {
            ItemStackContainer contents = new ItemStackContainer(null, bag, LAYOUT.slots());
            ItemStack toAdd = casing.copy();
            while (carried >= 1F) {
                if (!place(contents, toAdd)) break;
                carried -= 1F;
                took = true;
            }
            contents.setChanged();
        }

        progress.put(key, carried);
        bag.set(ModDataComponents.CASING_PROGRESS.get(), Map.copyOf(progress));
        return took;
    }

    public static void collectCasings(
            @Nullable Container inventory, BulletConfig config, int shots) {
        if (config.casingItem == null
                || config.casingAmount <= 0
                || !(inventory instanceof Inventory player)) return;
        float fraction = 1F / config.casingAmount * 0.5F * shots;
        ItemStack casing = null;
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack bag = player.getItem(slot);
            if (!(bag.getItem() instanceof CasingBagItem)) continue;
            if (casing == null) casing = config.casingItem.get();
            if (pushCasing(bag, casing, fraction)) return;
        }
    }

    private static boolean place(ItemStackContainer contents, ItemStack toAdd) {
        boolean any = false;
        for (int slot = 0; slot < contents.getContainerSize() && toAdd.getCount() > 0; slot++) {
            ItemStack held = contents.getItem(slot);
            if (held.isEmpty() || !ItemStack.isSameItemSameComponents(held, toAdd)) continue;
            int room = Math.min(toAdd.getCount(), held.getMaxStackSize() - held.getCount());
            if (room <= 0) continue;
            toAdd.shrink(room);
            held.grow(room);
            any = true;
        }
        for (int slot = 0; slot < contents.getContainerSize() && toAdd.getCount() > 0; slot++) {
            if (!contents.getItem(slot).isEmpty()) continue;
            contents.setItem(slot, toAdd.copy());
            toAdd.setCount(0);
            return true;
        }
        return any;
    }

    private static String key(ItemStack casing) {
        return BuiltInRegistries.ITEM.getKey(casing.getItem()).toString();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            player.openMenu(
                    new SimpleMenuProvider(
                            (id, inv, opener) ->
                                    new ItemBoxMenu(
                                            ModMenus.CASING_BAG.get(),
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
