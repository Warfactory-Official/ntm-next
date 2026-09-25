// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.ModDataComponents;
import com.hbm.packet.toclient.PlayerInformPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.network.RTTYSystem.RTTYChannel;
import com.hbm.tileentity.network.RTTYSystem;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class ItemRTTYPager extends Item {

    public static final String KEY_CHANNEL = "chan";
    public static final int ID_PAGER_DYN = 1_000;
    public static final int MAX_CHANNEL_LENGTH = 15;

    public static Consumer<Player> OPEN_SCREEN = player -> {};

    public ItemRTTYPager(Properties properties) {
        super(properties);
    }

    public static String channel(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.RTTY_PAGER_CHANNEL.get(), "");
    }

    public static void setChannel(ItemStack stack, String channel) {
        stack.set(ModDataComponents.RTTY_PAGER_CHANNEL.get(), channel);
    }

    public static void tickInventory(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            List<ItemStack> inventory = player.getInventory().getNonEquipmentItems();
            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack stack = inventory.get(slot);
                if (stack.getItem() instanceof ItemRTTYPager) tickStack(stack, player, slot);
            }
        }
    }

    private static void tickStack(ItemStack stack, ServerPlayer player, int slot) {
        if (!stack.has(ModDataComponents.RTTY_PAGER_CHANNEL.get())) return;

        ServerLevel level = player.level();
        String channel = channel(stack);
        RTTYChannel receiver = RTTYSystem.listen(level, channel);
        if (receiver == null || receiver.timeStamp < level.getGameTime() - 1) return;

        if ("selfdestruct".equals(receiver.signal + "")) {
            ExplosionVNT explosion =
                    new ExplosionVNT(
                            level,
                            player.getX(),
                            player.getY() + player.getBbHeight() / 2,
                            player.getZ(),
                            5,
                            null);
            explosion.setEntityProcessor(
                    new EntityProcessorCrossSmooth(1, 50).setupPiercing(5F, 0.5F));
            explosion.setPlayerProcessor(new PlayerProcessorStandard());
            explosion.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
            explosion.explode();
            stack.shrink(1);
            return;
        }

        int alive = player.tickCount % 1_000;
        String message =
                ChatFormatting.GOLD
                        + "[ "
                        + channel
                        + " ("
                        + alive
                        + ") ] "
                        + ChatFormatting.YELLOW
                        + receiver.signal;
        Services.NETWORK.sendTo(
                new PlayerInformPayload(message, ID_PAGER_DYN + slot, 5_000), player);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {

        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.isClientSide()) OPEN_SCREEN.accept(player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        if (!stack.has(ModDataComponents.RTTY_PAGER_CHANNEL.get()) || channel(stack).isEmpty()) {
            adder.accept(
                    Component.translatable("desc.item.rttyPager.noChannelSet")
                            .withStyle(ChatFormatting.RED));
        } else {
            adder.accept(
                    Component.translatable("desc.item.rttyPager.channel", channel(stack))
                            .withStyle(ChatFormatting.YELLOW));
        }
    }
}
