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

package com.hbm.items.special;

import java.util.Locale;
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

public class HolotapeImageItem extends Item {

    public static Consumer<InteractionHand> OPEN_SCREEN = hand -> {};

    public final EnumHoloImage type;

    public HolotapeImageItem(Properties properties, EnumHoloImage type) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) OPEN_SCREEN.accept(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(
                Component.translatable(
                        "desc.item.holotapeImage.bandColor",
                        Component.translatable("desc.item.holotapeImage." + type.color)
                                .withStyle(type.colorCode)));
        adder.accept(
                Component.translatable(
                        "desc.item.holotapeImage.label", Component.literal(type.label)));
    }

    public enum EnumHoloImage {
        HOLO_DIGAMMA(ChatFormatting.RED, "crimson", "D#"),
        HOLO_RESTORED(ChatFormatting.RED, "crimson", "D0"),
        HOLO_FE_HALL(ChatFormatting.GREEN, "lime", "001-HALL"),
        HOLO_FE_CORRIDOR(ChatFormatting.GREEN, "lime", "002-CORRIDOR"),
        HOLO_FE_SERVER(ChatFormatting.GREEN, "lime", "003-SERVER"),
        HOLO_FEH_DOME(ChatFormatting.RED, "red", "011-DOME"),
        HOLO_FEH_BOAT(ChatFormatting.RED, "red", "012-BOAT"),
        HOLO_FEH_LSC(ChatFormatting.RED, "red", "013-LAUNCH"),
        HOLO_F3_RC(ChatFormatting.DARK_GREEN, "green", "021-RIVET"),
        HOLO_F3_IV(ChatFormatting.DARK_GREEN, "green", "022-V87"),
        HOLO_F3_WM(ChatFormatting.DARK_GREEN, "green", "023-MONUMENT"),
        HOLO_NV_CRATER(ChatFormatting.GOLD, "brown", "031-MOUNTAIN"),
        HOLO_NV_DIVIDE(ChatFormatting.GOLD, "brown", "032-ROAD"),
        HOLO_NV_BM(ChatFormatting.GOLD, "brown", "033-BROADCAST"),
        HOLO_O_1(ChatFormatting.WHITE, "chroma", "X00-TRANSCRIPT"),
        HOLO_O_2(ChatFormatting.WHITE, "chroma", "X01-NEWS"),
        HOLO_O_3(ChatFormatting.WHITE, "chroma", "X02-FICTION"),
        HOLO_CHALLENGE(ChatFormatting.GRAY, "none", "-");

        public final ChatFormatting colorCode;
        public final String color;
        public final String label;

        EnumHoloImage(ChatFormatting colorCode, String color, String label) {
            this.colorCode = colorCode;
            this.color = color;
            this.label = label;
        }

        public String textKey() {
            String[] words = name().toLowerCase(Locale.ROOT).split("_");
            StringBuilder key = new StringBuilder("desc.item.holotapeImage.").append(words[0]);
            for (int i = 1; i < words.length; i++) {
                key.append(Character.toUpperCase(words[i].charAt(0))).append(words[i].substring(1));
            }
            return key.toString();
        }
    }
}
