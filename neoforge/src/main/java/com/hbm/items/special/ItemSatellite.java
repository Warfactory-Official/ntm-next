// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.items.ISatChip;
import com.hbm.lib.Library;
import com.hbm.registration.OverridePoint;
import com.hbm.saveddata.satellites.SatelliteType;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class ItemSatellite extends Item implements ISatChip {
    public static final OverridePoint<Describer> DESCRIBE =
            OverridePoint.of(
                    Library.id("satellite_describe"),
                    (stack, type, context, flag, adder, ntm) -> ntm.run());

    public static final OverridePoint<Use> USE =
            OverridePoint.of(
                    Library.id("satellite_use"),
                    (stack, type, level, player, hand) -> InteractionResult.PASS);

    @FunctionalInterface
    public interface Describer {
        void describe(
                ItemStack stack,
                SatelliteType type,
                TooltipContext context,
                TooltipFlag flag,
                Consumer<Component> adder,
                Runnable ntm);
    }

    @FunctionalInterface
    public interface Use {
        InteractionResult use(
                ItemStack stack,
                @Nullable SatelliteType type,
                Level level,
                Player player,
                InteractionHand hand);
    }

    public ItemSatellite(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        Runnable ntm =
                () ->
                        adder.accept(
                                Component.translatable("satchip.frequency")
                                        .append(": " + getFreq(stack))
                                        .withStyle(ChatFormatting.AQUA));
        SatelliteType type = SatelliteType.fromStack(stack);
        if (type == null) ntm.run();
        else DESCRIBE.get().describe(stack, type, context, flag, adder, ntm);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        SatelliteType type = SatelliteType.fromStack(stack);
        InteractionResult result = USE.get().use(stack, type, level, player, hand);
        return type == null && result == InteractionResult.PASS
                ? super.use(level, player, hand)
                : result;
    }
}
