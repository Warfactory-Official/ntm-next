/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

import com.hbm.handler.UnstableFuses;
import com.hbm.inventory.ItemStackContainer;
import com.hbm.inventory.container.ItemBoxMenu;
import com.hbm.inventory.container.ModMenus;
import com.hbm.items.ModDataComponents;
import com.hbm.platform.Platform;
import com.hbm.util.KeyHints;
import com.hbm.util.RegistryUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class ToolBoxItem extends Item {
    public static final ItemBoxMenu.Layout LAYOUT =
            new ItemBoxMenu.Layout(
                    3,
                    8,
                    17,
                    49,
                    8,
                    129,
                    187,
                    176,
                    211,
                    37,
                    0x404040,
                    117,
                    RegistryUtil.id("textures/gui/gui_toolbox.png"));
    private static final int ROW = 8;
    private static final int HOTBAR = 9;

    public ToolBoxItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (!player.isShiftKeyDown()) {
            moveRows(stack, player);
            return InteractionResult.SUCCESS;
        }
        stack.set(ModDataComponents.TOOLBOX_OPEN.get(), true);
        player.openMenu(
                new SimpleMenuProvider(
                        (id, inv, opener) ->
                                new ItemBoxMenu(
                                        ModMenus.TOOLBOX.get(),
                                        id,
                                        inv,
                                        LAYOUT,
                                        new ItemStackContainer(opener, stack, LAYOUT.slots()) {
                                            @Override
                                            public void stopOpen(ContainerUser user) {
                                                super.stopOpen(user);
                                                stack.remove(ModDataComponents.TOOLBOX_OPEN.get());
                                            }
                                        }),
                        stack.getHoverName()));
        return InteractionResult.SUCCESS;
    }

    private static void moveRows(ItemStack box, Player player) {
        Inventory inventory = player.getInventory();
        int held = inventory.getSelectedSlot();

        NonNullList<ItemStack> stacks = NonNullList.withSize(LAYOUT.slots(), ItemStack.EMPTY);
        box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(stacks);

        ItemStack[] outgoing = new ItemStack[ROW];
        int extra = 0;
        for (int slot = 0; slot < HOTBAR; slot++) {
            if (slot == held) continue;
            ItemStack carried = inventory.getItem(slot);
            if (!carried.isEmpty() && !Platform.canFitInsideContainerItems(carried)) {
                extra++;
                player.drop(carried, true, Prediction.SERVER_ONLY);
                inventory.setItem(slot, ItemStack.EMPTY);
                continue;
            }
            outgoing[boxSlot(slot, held)] = carried;
        }
        if (extra > 0) {
            player.sendSystemMessage(
                    (extra == 1
                                    ? Component.translatable("chat.toolbox.nested")
                                    : Component.translatable("chat.toolbox.nested.many", extra))
                            .withStyle(ChatFormatting.RED));
        }

        List<Integer> active = activeRows(stacks);
        int lowestActive = Integer.MAX_VALUE;
        int lowestInactive = Integer.MAX_VALUE;
        for (int row = 0; row < LAYOUT.rows(); row++) {
            if (active.contains(row)) lowestActive = Math.min(row, lowestActive);
            else lowestInactive = Math.min(row, lowestInactive);
        }
        lowestInactive =
                lowestInactive > LAYOUT.rows() - 1
                        ? LAYOUT.rows() - 1
                        : Math.max(0, lowestInactive - 1);

        NonNullList<ItemStack> ending = NonNullList.withSize(LAYOUT.slots(), ItemStack.EMPTY);
        ItemStack[] incoming = new ItemStack[ROW];
        for (int row : active) {
            if (row == lowestActive) {
                for (int i = 0; i < ROW; i++) incoming[i] = stacks.get(row * ROW + i);
                continue;
            }
            for (int i = 0; i < ROW; i++)
                ending.set((row - 1) * ROW + i, stacks.get(row * ROW + i));
        }
        for (int i = 0; i < ROW; i++) {
            ending.set(
                    lowestInactive * ROW + i, outgoing[i] == null ? ItemStack.EMPTY : outgoing[i]);
        }

        for (int slot = 0; slot < HOTBAR; slot++) {
            if (slot == held) continue;
            ItemStack arriving = incoming[boxSlot(slot, held)];
            inventory.setItem(slot, arriving == null ? ItemStack.EMPTY : arriving);
        }
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(ending));
        UnstableFuses.changed(player);
    }

    private static int boxSlot(int hotbarSlot, int held) {
        return hotbarSlot > held ? hotbarSlot - 1 : hotbarSlot;
    }

    private static List<Integer> activeRows(NonNullList<ItemStack> stacks) {
        List<Integer> rows = new ArrayList<>();
        for (int row = 0; row < LAYOUT.rows(); row++) {
            for (int slot = 0; slot < ROW; slot++) {
                if (!stacks.get(row * ROW + slot).isEmpty()) {
                    rows.add(row);
                    break;
                }
            }
        }
        return rows;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(
                Component.translatable(
                        "tooltip.hbm.toolbox.swap", KeyHints.key("key.use", "key.mouse.right")));
        adder.accept(
                Component.translatable(
                        "tooltip.hbm.toolbox.open",
                        KeyHints.key("key.sneak", "key.keyboard.left.shift"),
                        KeyHints.key("key.use", "key.mouse.right")));
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
