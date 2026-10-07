// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.items.ModDataComponents;
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
import org.jspecify.annotations.Nullable;

public class ItemReactorSensor extends Item {
    @FunctionalInterface
    public interface Target {
        InteractionResult useOn(UseOnContext context, @Nullable BlockPos core);
    }

    private static volatile List<Target> targets = List.of();

    public ItemReactorSensor(Properties props) {
        super(props);
    }

    public static synchronized void addonTarget(Target target) {
        List<Target> next = new ArrayList<>(targets);
        next.add(target);
        targets = List.copyOf(next);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockPos core = MultiblockSurface.coreOfAny(level, pos, level.getBlockState(pos));
        if (core != null && level.getBlockState(core).is(ModBlocks.REACTOR_RESEARCH.get())) {
            return recordPosition(
                    context, Component.translatable("chat.reactorSensor.positionSet"));
        }
        for (Target target : targets) {
            InteractionResult result = target.useOn(context, core);
            if (result != InteractionResult.PASS) return result;
        }
        return InteractionResult.PASS;
    }

    public static InteractionResult recordPosition(UseOnContext context, Component message) {
        BlockPos pos = context.getClickedPos();
        context.getItemInHand().set(ModDataComponents.REACTOR_POS.get(), pos.asLong());
        Player player = context.getPlayer();
        if (!context.getLevel().isClientSide() && player != null) announce(player, message);
        confirm(context);
        return InteractionResult.SUCCESS;
    }

    public static void announce(Player player, Component message) {
        player.sendSystemMessage(
                Component.literal("[")
                        .append(Component.translatable("item.hbm.reactor_sensor"))
                        .append("] ")
                        .withStyle(ChatFormatting.DARK_AQUA)
                        .append(message.copy().withStyle(ChatFormatting.GREEN)));
    }

    public static void confirm(UseOnContext context) {
        context.getLevel()
                .playSound(
                        null,
                        context.getClickedPos(),
                        ModSounds.TECH_BOOP.get(),
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F);
        Player player = context.getPlayer();
        if (player != null) player.swing(InteractionHand.MAIN_HAND);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        Long packed = stack.get(ModDataComponents.REACTOR_POS.get());
        if (packed == null) {
            adder.accept(Component.translatable("desc.item.reactorSensor.noReactor"));
            return;
        }
        BlockPos pos = BlockPos.of(packed);
        adder.accept(Component.literal("x: " + pos.getX()));
        adder.accept(Component.literal("y: " + pos.getY()));
        adder.accept(Component.literal("z: " + pos.getZ()));
    }
}
