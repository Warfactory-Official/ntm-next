// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.util.I18nUtil;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public final class ItemGuideBook extends Item {

    public static Consumer<BookType> OPEN_SCREEN;
    public final BookType type;

    public ItemGuideBook(Properties properties, BookType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) OPEN_SCREEN.accept(type);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.literal(String.join(" ", I18nUtil.resolveKeyArray(type.title))));
    }

    public enum BookType {
        TEST("book.test.cover", 2F),
        RBMK("book.rbmk.cover", 1.5F),
        HADRON("book.error.cover", 1.5F),
        STARTER("book.starter.cover", 1.5F);

        public final String title;
        public final float titleScale;

        BookType(String title, float titleScale) {
            this.title = title;
            this.titleScale = titleScale;
        }
    }
}
