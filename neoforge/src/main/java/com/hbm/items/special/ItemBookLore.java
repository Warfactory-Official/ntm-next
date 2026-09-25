// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class ItemBookLore extends Item {

    public static final int DEFAULT_COVER = 0x303030;
    public static final int DEFAULT_TITLE = 0xFFFFFF;

    public static final String FALLBACK_KEY = "test";

    public static Consumer<Player> OPEN_SCREEN = player -> {};

    public ItemBookLore(Properties properties) {
        super(properties);
    }

    public static ItemStack createBook(String key, int pages, int coverColor, int titleColor) {
        ItemStack book = new ItemStack(ModItems.BOOK_LORE);
        book.set(ModDataComponents.LORE_BOOK_KEY.get(), key);
        book.set(ModDataComponents.LORE_BOOK_PAGES.get(), pages);
        book.set(ModDataComponents.LORE_BOOK_COVER.get(), coverColor);
        book.set(ModDataComponents.LORE_BOOK_TITLE.get(), titleColor);
        return book;
    }

    public static void addArgs(ItemStack book, int page, String... args) {
        Map<String, List<String>> all =
                new HashMap<>(book.getOrDefault(ModDataComponents.LORE_BOOK_ARGS.get(), Map.of()));
        all.put(String.valueOf(page), List.of(args));
        book.set(ModDataComponents.LORE_BOOK_ARGS.get(), Map.copyOf(all));
    }

    public static List<String> argsOf(ItemStack stack, int page) {
        return stack.getOrDefault(
                        ModDataComponents.LORE_BOOK_ARGS.get(), Map.<String, List<String>>of())
                .getOrDefault(String.valueOf(page), List.of());
    }

    public static String keyOf(ItemStack stack) {
        String key = stack.get(ModDataComponents.LORE_BOOK_KEY.get());
        return key == null || key.isEmpty() ? FALLBACK_KEY : key;
    }

    public static int pagesOf(ItemStack stack) {
        Integer pages = stack.get(ModDataComponents.LORE_BOOK_PAGES.get());
        return pages == null ? 0 : pages;
    }

    public static int coverColorOf(ItemStack stack) {
        Integer color = stack.get(ModDataComponents.LORE_BOOK_COVER.get());
        return color == null || color <= 0 ? DEFAULT_COVER : color;
    }

    public static int titleColorOf(ItemStack stack) {
        Integer color = stack.get(ModDataComponents.LORE_BOOK_TITLE.get());
        return color == null || color <= 0 ? DEFAULT_TITLE : color;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("book_lore." + keyOf(stack) + ".name");
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> lines,
            TooltipFlag flag) {

        String stackKey = stack.get(ModDataComponents.LORE_BOOK_KEY.get());
        if (stackKey == null || stackKey.isEmpty()) {
            super.appendHoverText(stack, context, display, lines, flag);
            return;
        }

        String key = "book_lore." + stackKey + ".author";
        Component author = Component.translatable(key);
        if (!author.getString().equals(key)) {
            lines.accept(
                    Component.translatable("book_lore.author", author)
                            .withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, display, lines, flag);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {

        if (level.isClientSide()) OPEN_SCREEN.accept(player);
        return InteractionResult.SUCCESS;
    }
}
