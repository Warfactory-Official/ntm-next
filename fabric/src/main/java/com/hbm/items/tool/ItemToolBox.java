// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.inventory.ItemStackContainer;
import com.hbm.inventory.container.MenuItemBox;
import com.hbm.inventory.container.ModMenus;
import com.hbm.items.ModDataComponents;
import com.hbm.lib.Library;
import com.hbm.platform.Services;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
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

public class ItemToolBox extends Item {

    public static final MenuItemBox.Layout LAYOUT =
            new MenuItemBox.Layout(
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
                    Library.id("textures/gui/gui_toolbox.png"));
    private static final int ROW = 8;

    private static final int HOTBAR = 9;

    public ItemToolBox(Properties properties) {
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
                                new MenuItemBox(
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
            if (!carried.isEmpty() && !Services.PLATFORM.canFitInsideContainerItems(carried)) {
                extra++;
                player.drop(carried, true);
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
        adder.accept(Component.translatable("desc.item.toolbox.clickToSwap"));
        adder.accept(Component.translatable("desc.item.toolbox.shiftClickToOpen"));
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
