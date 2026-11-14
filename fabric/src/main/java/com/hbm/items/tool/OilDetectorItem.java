/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.items.tool;

import com.hbm.blocks.ModBlocks;
import com.hbm.packet.ClientEventConstants;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.PlayerInformLayout;
import com.hbm.registration.OverridePoint;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import com.hbm.util.RegistryUtil;
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

public class OilDetectorItem extends Item {
    public static final OverridePoint<Reading> READING =
            OverridePoint.of(RegistryUtil.id("oil_detector_reading"), OilDetectorItem::scan);

    public OilDetectorItem(Properties properties) {
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
                    PlayerInformLayout.encode(
                            result,
                            ClientEventConstants.PLAYER_INFORM_ID_DETONATOR,
                            ClientEventConstants.PLAYER_INFORM_DEFAULT_MILLIS),
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
