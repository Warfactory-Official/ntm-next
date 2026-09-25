// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.block;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public final class ItemBlockDiamondGravel extends BlockItem {

    public ItemBlockDiamondGravel(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("desc.item.diamondGravel.thereIsSomeKind"));
        adder.accept(Component.translatable("desc.item.diamondGravel.butICanT"));
        adder.accept(Component.empty());
        adder.accept(Component.translatable("desc.item.diamondGravel.update20200704"));
        adder.accept(Component.translatable("desc.item.diamondGravel.weDenyAnyImplications"));
        adder.accept(Component.translatable("desc.item.diamondGravel.theBasisThatIt"));
        adder.accept(Component.translatable("desc.item.diamondGravel.thatPeopleStartedStabbing"));
        adder.accept(Component.empty());
        adder.accept(Component.translatable("desc.item.diamondGravel.update20201704"));
        adder.accept(Component.translatable("desc.item.diamondGravel.asItTurnsOut"));
        adder.accept(Component.translatable("desc.item.diamondGravel.neverReallyAThing"));
        adder.accept(Component.translatable("desc.item.diamondGravel.haveBeenAJoke"));
        adder.accept(Component.translatable("desc.item.diamondGravel.weApologizeForGetting"));
        adder.accept(Component.translatable("desc.item.diamondGravel.thisNonJokeThat"));
        adder.accept(Component.empty());
        adder.accept(Component.translatable("desc.item.diamondGravel.iAddedAnItem"));
    }
}
