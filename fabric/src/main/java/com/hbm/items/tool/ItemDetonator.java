// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.NuclearTech;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.NtmContracts;
import com.hbm.interfaces.IBomb.BombReturnCode;
import com.hbm.interfaces.IBomb;
import com.hbm.items.ModDataComponents;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
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

public class ItemDetonator extends Item {

    public ItemDetonator(Properties props) {
        super(props);
    }

    static BlockPos bombTarget(Level level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core == null ? pos : core;
    }

    private static void message(Player player, Component body) {
        player.sendSystemMessage(
                Component.literal("[")
                        .append(Component.translatable("item.hbm.detonator"))
                        .append("] ")
                        .withStyle(ChatFormatting.DARK_AQUA)
                        .append(body));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Level level = context.getLevel();
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                BlockPos pos = context.getClickedPos();
                context.getItemInHand().set(ModDataComponents.DETONATOR_POS.get(), pos.asLong());
                level.playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        ModSounds.TECH_BOOP.get(),
                        SoundSource.PLAYERS,
                        2.0F,
                        1.0F);
                message(
                        player,
                        Component.translatable("item.hbm.detonator.set")
                                .withStyle(ChatFormatting.GREEN));
            }
            return InteractionResult.SUCCESS;
        }

        return detonate(level, player, context.getItemInHand());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return detonate(level, player, player.getItemInHand(hand));
    }

    private InteractionResult detonate(Level level, Player player, ItemStack stack) {
        Long packed = stack.get(ModDataComponents.DETONATOR_POS.get());
        if (packed == null) {
            if (!level.isClientSide())
                message(
                        player,
                        Component.translatable("item.hbm.detonator.nopos")
                                .withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        BlockPos pos = bombTarget(level, BlockPos.of(packed));
        IBomb bomb = NtmContracts.BOMB.at(level, pos);
        if (bomb == null) {
            if (!level.isClientSide())
                message(
                        player,
                        Component.translatable(BombReturnCode.ERROR_NO_BOMB.getUnlocalizedMessage())
                                .withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.TECH_BLEEP.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
            BombReturnCode ret = bomb.explode(level, pos, player);
            if (Services.CONFIG.runtime().extendedLogging()) {
                NuclearTech.LOGGER.info(
                        "[DET] Tried to detonate block at {} / {} / {} by {}!",
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        player.getDisplayName().getString());
            }
            message(
                    player,
                    Component.translatable(ret.getUnlocalizedMessage())
                            .withStyle(
                                    ret.wasSuccessful()
                                            ? ChatFormatting.YELLOW
                                            : ChatFormatting.RED));
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
        adder.accept(Component.translatable("item.hbm.detonator.desc1"));
        adder.accept(Component.translatable("item.hbm.detonator.desc2"));
        Long packed = stack.get(ModDataComponents.DETONATOR_POS.get());
        if (packed == null) {
            adder.accept(
                    Component.translatable("item.hbm.detonator.tooltip_nopos")
                            .withStyle(ChatFormatting.RED));
        } else {
            BlockPos pos = BlockPos.of(packed);
            adder.accept(
                    Component.translatable(
                                    "item.hbm.detonator.linked", pos.getX(), pos.getY(), pos.getZ())
                            .withStyle(ChatFormatting.YELLOW));
        }
    }
}
