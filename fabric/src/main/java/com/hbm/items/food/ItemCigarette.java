// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import com.hbm.advancement.HbmCriteria;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemCustomLore;
import com.hbm.packet.toclient.VomitPayload;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.sound.ModSounds;
import com.hbm.util.GameTime;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class ItemCigarette extends ItemCustomLore {

    private static final ChatFormatting[] GREAT_COLORS = {
        ChatFormatting.RED, ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.GREEN,
        ChatFormatting.AQUA, ChatFormatting.BLUE, ChatFormatting.DARK_PURPLE,
                ChatFormatting.LIGHT_PURPLE
    };

    private final Effect effect;

    public ItemCigarette(Properties properties, Effect effect) {
        super(properties);
        this.effect = effect;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 30;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!entity.level().isClientSide()) {

            ItemStack smoked = stack.copyWithCount(1);
            stack.shrink(1);
            level.playSound(
                    null,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    ModSounds.PLAYER_COUGH.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
            if (entity instanceof ServerPlayer player) {
                effect.apply(player);
                HbmCriteria.smoked(player, smoked);
            }
            ParticleCreators.vomit(level, entity, VomitPayload.MODE_SMOKE, 30);
        }
        return stack;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        if (this == ModItems.CIGARETTE.get()) {
            adder.accept(
                    Component.translatable("desc.item.cigarette.asbestosFilter")
                            .withStyle(ChatFormatting.RED));
            adder.accept(
                    Component.translatable("desc.item.cigarette.highInTar")
                            .withStyle(ChatFormatting.RED));
            adder.accept(
                    Component.translatable("desc.item.cigarette.tobaccoContains100Polonium")
                            .withStyle(ChatFormatting.RED));
            adder.accept(
                    Component.translatable("desc.item.cigarette.yum")
                            .withStyle(ChatFormatting.RED));
            return;
        }
        long len = 2_000L;
        ChatFormatting color =
                GREAT_COLORS[(int) (GameTime.now() % len * GREAT_COLORS.length / len)];
        adder.accept(
                Component.translatable("desc.item.cigarette.thisCanTBe")
                        .append(
                                Component.translatable("desc.item.cigarette.great")
                                        .withStyle(color)));
    }

    public interface Effect {
        void apply(ServerPlayer player);
    }
}
