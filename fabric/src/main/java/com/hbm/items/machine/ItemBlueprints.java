// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ItemBlueprints extends Item {

    public ItemBlueprints(Properties properties) {
        super(properties);
    }

    @Nullable
    public static String grabPool(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemBlueprints)) return null;
        return stack.get(ModDataComponents.BLUEPRINT_POOL.get());
    }

    public static ItemStack make(String pool) {
        ItemStack stack = new ItemStack(ModItems.BLUEPRINTS);
        stack.set(ModDataComponents.BLUEPRINT_POOL.get(), pool);
        return stack;
    }

    private static boolean consumePaper(Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(Items.PAPER)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.PASS;

        ItemStack stack = player.getItemInHand(hand);
        String pool = grabPool(stack);
        if (pool == null) return InteractionResult.PASS;
        if (pool.startsWith(GenericRecipes.POOL_PREFIX_SECRET)) return InteractionResult.PASS;
        if (!consumePaper(player)) return InteractionResult.PASS;

        player.swing(hand);

        ItemStack copy = stack.copy();
        copy.setCount(1);

        if (!player.isCreative()) {
            if (stack.getCount() < stack.getMaxStackSize()) {
                stack.grow(1);
            } else {
                player.getInventory().placeItemBackInInventory(copy);
            }
        } else {
            player.drop(copy, false);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        String pool = grabPool(stack);
        if (pool == null) return;

        List<String> poolRecipes = GenericRecipes.pools().get(pool);
        if (poolRecipes == null || poolRecipes.isEmpty()) return;

        if (pool.startsWith(GenericRecipes.POOL_PREFIX_SECRET)) {
            adder.accept(
                    Component.translatable("desc.item.blueprints.cannotBeCopied")
                            .withStyle(ChatFormatting.RED));
        } else {
            adder.accept(
                    Component.translatable("desc.item.blueprints.rightClickToCopy")
                            .withStyle(ChatFormatting.YELLOW));
        }

        for (String name : poolRecipes) {
            GenericRecipe recipe = GenericRecipes.pooled(name);
            if (recipe != null) adder.accept(Component.literal(recipe.getLocalizedName()));
        }
    }
}
