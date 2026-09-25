// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.items.ModDataComponents;
import com.hbm.tileentity.network.IDroneLinkable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ItemDroneLinker extends Item {

    public ItemDroneLinker(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof IDroneLinkable clicked))
            return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        Long stored = stack.get(ModDataComponents.DRONE_LINK_TARGET.get());

        if (stored == null) {
            say(player, stack, "Set initial position!", ChatFormatting.AQUA);
        } else if (level.getBlockEntity(BlockPos.of(stored)) instanceof IDroneLinkable previous) {
            previous.setNextTarget(clicked.getPoint());
            say(player, stack, "Link set!", ChatFormatting.AQUA);
        } else {
            say(player, stack, "Previous link lost!", ChatFormatting.RED);
        }

        stack.set(ModDataComponents.DRONE_LINK_TARGET.get(), pos.asLong());
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !stack.has(ModDataComponents.DRONE_LINK_TARGET.get())) {
            return InteractionResult.PASS;
        }

        stack.remove(ModDataComponents.DRONE_LINK_TARGET.get());
        say(player, stack, "Position cleared!", ChatFormatting.GREEN);
        return InteractionResult.SUCCESS;
    }

    private static void say(
            @Nullable Player player, ItemStack stack, String message, ChatFormatting colour) {
        if (player == null) return;
        player.sendSystemMessage(
                Component.literal("[")
                        .withStyle(ChatFormatting.DARK_AQUA)
                        .append(stack.getHoverName().copy().withStyle(ChatFormatting.DARK_AQUA))
                        .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA))
                        .append(Component.literal(message).withStyle(colour)));
    }
}
