// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.blocks.ModBlocks;
import com.hbm.lib.Library;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toclient.PlayerInformPayload;
import com.hbm.registration.OverridePoint;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class ItemOilDetector extends Item {
    public static final OverridePoint<Reading> READING =
            OverridePoint.of(Library.id("oil_detector_reading"), ItemOilDetector::scan);

    public ItemOilDetector(Properties properties) {
        super(properties);
    }

    private static boolean containsOil(Level level, int x, int z, int topY, int floorExclusive) {
        for (int y = topY; y > floorExclusive; y--) {
            if (level.getBlockState(new BlockPos(x, y, z)).is(ModBlocks.ORE_OIL.get())) return true;
        }
        return false;
    }

    private static Component scan(Level level, Player player) {
        int x = (int) player.getX();
        int y = (int) player.getY();
        int z = (int) player.getZ();

        boolean direct = containsOil(level, x, z, y + 15, 5);
        boolean oil =
                containsOil(level, x + 5, z, y + 15, 5)
                        || containsOil(level, x - 5, z, y + 15, 5)
                        || containsOil(level, x, z + 5, y + 15, 5)
                        || containsOil(level, x, z - 5, y + 15, 5)
                        || containsOil(level, x + 10, z, y + 15, 10)
                        || containsOil(level, x - 10, z, y + 15, 10)
                        || containsOil(level, x, z + 10, y + 15, 10)
                        || containsOil(level, x, z - 10, y + 15, 10)
                        || containsOil(level, x + 5, z + 5, y + 15, 5)
                        || containsOil(level, x - 5, z + 5, y + 15, 5)
                        || containsOil(level, x + 5, z - 5, y + 15, 5)
                        || containsOil(level, x - 5, z - 5, y + 15, 5);

        return direct
                ? Component.translatable("item.hbm.oil_detector.bullseye")
                        .withStyle(ChatFormatting.DARK_GREEN)
                : oil
                        ? Component.translatable("item.hbm.oil_detector.detected")
                                .withStyle(ChatFormatting.GOLD)
                        : Component.translatable("item.hbm.oil_detector.noOil")
                                .withStyle(ChatFormatting.RED);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            Component result = READING.get().read(level, player);
            PacketWire.sendTo(
                    new PlayerInformPayload(
                            result,
                            PlayerInformPayload.ID_DETONATOR,
                            PlayerInformPayload.DEFAULT_MILLIS),
                    serverPlayer);
            EntitySounds.play(
                    player,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.TECH_BLEEP.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @FunctionalInterface
    public interface Reading {
        Component read(Level level, Player player);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("item.hbm.oil_detector.desc1"));
        adder.accept(Component.translatable("item.hbm.oil_detector.desc2"));
    }
}
