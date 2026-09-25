// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.itempool;

import com.hbm.items.ModItems;
import com.hbm.items.special.ItemBookLore;
import com.hbm.lib.Library;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class LoreBooks {

    public record Book(String key, int pages, int coverColor, int titleColor) {

        public ItemStack stack() {
            return ItemBookLore.createBook(key, pages, coverColor, titleColor);
        }
    }

    public static final List<Book> OFFICE =
            List.of(
                    new Book("resignation_note", 3, 0x6BC8FF, 0x0A0A0A),
                    new Book("memo_stocks", 1, 0x6BC8FF, 0x0A0A0A),
                    new Book("memo_schrab_gsa", 2, 0x6BC8FF, 0x0A0A0A),
                    new Book("memo_schrab_rd", 4, 0x6BC8FF, 0x0A0A0A),
                    new Book("memo_schrab_nuke", 3, 0x6BC8FF, 0x0A0A0A));

    public static final List<Book> LAB =
            List.of(
                    new Book("bf_bomb_1", 4, 0x1E1E1E, 0x46EA44),
                    new Book("bf_bomb_2", 6, 0x1E1E1E, 0x46EA44),
                    new Book("bf_bomb_3", 6, 0x1E1E1E, 0x46EA44),
                    new Book("bf_bomb_4", 5, 0x1E1E1E, 0x46EA44),
                    new Book("bf_bomb_5", 9, 0x1E1E1E, 0x46EA44));

    public static final Book BEACON = new Book("beacon", 12, 0x404040, 0xD637B3);

    public static final List<String> KEYS =
            List.of(
                    "beacon",
                    "bf_bomb_1",
                    "bf_bomb_2",
                    "bf_bomb_3",
                    "bf_bomb_4",
                    "bf_bomb_5",
                    "book_dust",
                    "book_flower",
                    "book_iodine",
                    "book_mercury",
                    "book_phosphorous",
                    "book_syringe",
                    "memo_schrab_gsa",
                    "memo_schrab_nuke",
                    "memo_schrab_rd",
                    "memo_stocks",
                    "resignation_note",
                    "test");

    private LoreBooks() {}

    private static final List<Book> MKU =
            List.of(
                    new Book("book_iodine", 3, 0x271E44, 0xFBFFF4),
                    new Book("book_phosphorous", 2, 0x271E44, 0xFBFFF4),
                    new Book("book_dust", 3, 0x271E44, 0xFBFFF4),
                    new Book("book_mercury", 2, 0x271E44, 0xFBFFF4),
                    new Book("book_flower", 2, 0x271E44, 0xFBFFF4),
                    new Book("book_syringe", 2, 0x271E44, 0xFBFFF4));

    private static List<Item> mkuItems() {
        return List.of(
                byId("powder_iodine"),
                byId("powder_fire"),
                byId("dust"),
                byId("nugget_mercury"),
                byId("morning_glory"),
                byId("syringe_metal_empty"));
    }

    private static Item byId(String path) {
        return BuiltInRegistries.ITEM
                .getOptional(Library.id(path))
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "MKU lore needs hbm:"
                                                + path
                                                + ", which is not registered"));
    }

    public static Item mkuItem(RandomSource random) {
        return mkuItems().get(random.nextInt(6));
    }

    public static List<Item> mkuLayout(long worldSeed) {
        Layout cached = layout;
        if (cached != null && cached.seed() == worldSeed) return cached.slots();

        List<Item> slots = new ArrayList<>();
        slots.addAll(mkuItems());
        slots.add(null);
        slots.add(null);
        slots.add(null);
        Collections.shuffle(slots, new Random(worldSeed));

        List<Item> fixed = Collections.unmodifiableList(slots);
        layout = new Layout(worldSeed, fixed);
        return fixed;
    }

    private record Layout(long seed, List<Item> slots) {}

    private static volatile @Nullable Layout layout;

    public static ItemStack mkuBook(long worldSeed, Item mkuItem) {
        int index = -1;
        List<Item> items = mkuItems();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) == mkuItem) {
                index = i;
                break;
            }
        }
        if (index < 0) return new ItemStack(ModItems.FLAME_PONY);

        Book book = MKU.get(index);
        List<Item> layout = mkuLayout(worldSeed);
        int slot = 1;
        for (int i = 0; i < layout.size(); i++) {
            if (layout.get(i) == mkuItem) {
                slot = i + 1;
                break;
            }
        }
        ItemStack stack = book.stack();
        ItemBookLore.addArgs(stack, book.pages() - 1, String.valueOf(slot));
        return stack;
    }

    public static ItemStack office(RandomSource random) {
        return OFFICE.get(random.nextInt(OFFICE.size())).stack();
    }

    public static ItemStack lab(RandomSource random) {
        return LAB.get(random.nextInt(LAB.size())).stack();
    }
}
