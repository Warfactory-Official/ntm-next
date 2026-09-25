// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.api.item.IDesignatorItem;
import com.hbm.items.ModDataComponents;
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

public class ItemDesignatorManual extends Item implements IDesignatorItem {

    public static final int ADD = 0, SUBTRACT = 1, SET_HERE = 2;
    public static final int REFERENCE_X = 0, REFERENCE_Z = 1;

    public static Consumer<InteractionHand> OPEN_SCREEN = hand -> {};

    public ItemDesignatorManual(Properties props) {
        super(props);
    }

    public static long apply(
            long target, int operator, int value, int reference, double posX, double posZ) {
        int x = IDesignatorItem.unpackX(target);
        int z = IDesignatorItem.unpackZ(target);
        if (operator == SET_HERE) {
            if (reference == REFERENCE_X) x = (int) Math.round(posX);
            else z = (int) Math.round(posZ);
        } else {
            int delta = operator == ADD ? value : operator == SUBTRACT ? -value : 0;
            if (reference == REFERENCE_X) x += delta;
            else z += delta;
        }
        return IDesignatorItem.pack(x, z);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) OPEN_SCREEN.accept(hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isReady(ItemStack stack) {
        return stack.has(ModDataComponents.TARGET_DESIGNATOR.get());
    }

    @Override
    public int getTargetX(ItemStack stack) {
        return IDesignatorItem.unpackX(
                stack.getOrDefault(ModDataComponents.TARGET_DESIGNATOR.get(), 0L));
    }

    @Override
    public int getTargetZ(ItemStack stack) {
        return IDesignatorItem.unpackZ(
                stack.getOrDefault(ModDataComponents.TARGET_DESIGNATOR.get(), 0L));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        if (isReady(stack)) {
            adder.accept(Component.translatable("item.hbm.designator.target_coord"));
            adder.accept(Component.literal("X: " + getTargetX(stack)));
            adder.accept(Component.literal("Z: " + getTargetZ(stack)));
        } else {
            adder.accept(Component.translatable("item.hbm.designator.choose_target"));
        }
    }
}
