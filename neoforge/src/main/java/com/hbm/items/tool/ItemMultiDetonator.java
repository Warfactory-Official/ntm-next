// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.NuclearTech;
import com.hbm.capability.NtmContracts;
import com.hbm.interfaces.IBomb;
import com.hbm.items.ModDataComponents;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ItemMultiDetonator extends Item {

    public ItemMultiDetonator(Properties props) {
        super(props);
    }

    private static List<Long> positions(ItemStack stack) {
        List<Long> stored = stack.get(ModDataComponents.DETONATOR_POS_LIST.get());
        return stored == null ? List.of() : stored;
    }

    private static void message(Player player, Component body) {
        player.sendSystemMessage(
                Component.literal("[")
                        .append(Component.translatable("item.hbm.detonator_multi"))
                        .append("] ")
                        .withStyle(ChatFormatting.DARK_AQUA)
                        .append(body));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        if (!player.isShiftKeyDown()) return InteractionResult.PASS;

        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        List<Long> grown = new ArrayList<>(positions(stack));
        grown.add(context.getClickedPos().asLong());
        stack.set(ModDataComponents.DETONATOR_POS_LIST.get(), List.copyOf(grown));

        level.playSound(
                null,
                player.blockPosition(),
                ModSounds.TECH_BOOP.get(),
                SoundSource.PLAYERS,
                2.0F,
                1.0F);
        if (!level.isClientSide()) {
            message(
                    player,
                    Component.translatable("item.hbm.detonator_multi.added")
                            .withStyle(ChatFormatting.GREEN));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        List<Long> linked = positions(stack);

        if (linked.isEmpty()) {
            if (!level.isClientSide()) {
                message(
                        player,
                        Component.translatable("item.hbm.detonator_multi.nopos")
                                .withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }

        if (player.isShiftKeyDown()) {
            stack.remove(ModDataComponents.DETONATOR_POS_LIST.get());
            level.playSound(
                    null,
                    player.blockPosition(),
                    ModSounds.TECH_BOOP.get(),
                    SoundSource.PLAYERS,
                    2.0F,
                    1.0F);
            if (!level.isClientSide()) {
                message(
                        player,
                        Component.translatable("item.hbm.detonator_multi.cleared")
                                .withStyle(ChatFormatting.RED));
            }
            return InteractionResult.SUCCESS;
        }

        int fired = 0;
        if (!level.isClientSide()) {
            for (long packed : linked) {

                BlockPos pos = ItemDetonator.bombTarget(level, BlockPos.of(packed));
                IBomb bomb = NtmContracts.BOMB.at(level, pos);
                if (bomb == null) continue;
                if (bomb.explode(level, pos, player).wasSuccessful()) fired++;
                if (Services.CONFIG.runtime().extendedLogging()) {
                    NuclearTech.LOGGER.info(
                            "[DET] Tried to detonate block at {} / {} / {} by {}!",
                            pos.getX(),
                            pos.getY(),
                            pos.getZ(),
                            player.getDisplayName().getString());
                }
            }
        }
        level.playSound(
                null,
                player.blockPosition(),
                ModSounds.TECH_BLEEP.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        if (!level.isClientSide()) {
            message(
                    player,
                    Component.translatable(
                                    "item.hbm.detonator_multi.triggered", fired, linked.size())
                            .withStyle(ChatFormatting.YELLOW));
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
        adder.accept(Component.translatable("item.hbm.detonator_multi.desc1"));
        adder.accept(Component.translatable("item.hbm.detonator_multi.desc2"));
        adder.accept(Component.translatable("item.hbm.detonator_multi.desc3"));

        List<Long> linked = positions(stack);
        if (linked.isEmpty()) {
            adder.accept(
                    Component.translatable("item.hbm.detonator_multi.nopos")
                            .withStyle(ChatFormatting.RED));
            return;
        }
        for (long packed : linked) {
            BlockPos pos = BlockPos.of(packed);
            adder.accept(
                    Component.translatable(
                                    "item.hbm.detonator_multi.pos",
                                    pos.getX(),
                                    pos.getY(),
                                    pos.getZ())
                            .withStyle(ChatFormatting.YELLOW));
        }
    }
}
