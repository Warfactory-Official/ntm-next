// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.items.ModDataComponents;
import com.hbm.sound.ModSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ItemTurretBiometry extends Item {

    public ItemTurretBiometry(Properties properties) {
        super(properties);
    }

    public static String @Nullable [] getNames(ItemStack stack) {
        List<String> names = stack.get(ModDataComponents.TURRET_WHITELIST.get());

        if (names == null || names.isEmpty()) return null;
        return names.toArray(new String[0]);
    }

    public static void addName(ItemStack stack, String s) {
        List<String> names =
                new ArrayList<>(
                        stack.getOrDefault(ModDataComponents.TURRET_WHITELIST.get(), List.of()));
        if (names.contains(s)) return;
        names.add(s);
        stack.set(ModDataComponents.TURRET_WHITELIST.get(), List.copyOf(names));
    }

    public static void clearNames(ItemStack stack) {
        stack.set(ModDataComponents.TURRET_WHITELIST.get(), List.of());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        addName(stack, player.getGameProfile().name());

        if (level.isClientSide())
            player.sendSystemMessage(
                    Component.translatable("desc.item.turretBiometry.addedPlayerData"));

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.TECH_BLEEP.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        player.swing(hand);

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        String[] names = getNames(stack);
        if (names == null) return;
        for (String name : names) adder.accept(Component.literal(name));
    }
}
