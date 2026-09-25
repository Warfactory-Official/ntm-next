// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.sound.ModSounds;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class ItemKitNBT extends Item {

    public ItemKitNBT(Properties properties) {
        super(properties);
    }

    public static ItemStack create(ItemStack... contents) {
        ItemStack stack = new ItemStack(ModItems.KIT_CUSTOM);
        stack.set(
                ModDataComponents.KIT_CONTENTS.get(),
                Arrays.stream(contents)
                        .filter(item -> !item.isEmpty())
                        .map(ItemStackTemplate::fromNonEmptyStack)
                        .toList());
        return stack;
    }

    public static List<ItemStackTemplate> contentsOf(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.KIT_CONTENTS.get(), List.of());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        for (ItemStackTemplate content : contentsOf(stack)) {
            player.getInventory().placeItemBackInInventory(content.create());
        }

        stack.shrink(1);

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.ITEM_UNPACK.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> lines,
            TooltipFlag flag) {
        List<ItemStackTemplate> contents = contentsOf(stack);
        if (contents.isEmpty()) return;

        lines.accept(Component.translatable("desc.shared.contains"));
        for (ItemStackTemplate template : contents) {
            ItemStack content = template.create();
            lines.accept(
                    Component.literal("-")
                            .append(content.getHoverName())
                            .append(content.getCount() > 1 ? " x" + content.getCount() : ""));
        }
    }
}
