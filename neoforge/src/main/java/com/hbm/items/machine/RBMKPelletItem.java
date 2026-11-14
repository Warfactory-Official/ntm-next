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

package com.hbm.items.machine;

import com.hbm.items.ModDataComponents;
import com.hbm.items.special.NuclearWasteItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class RBMKPelletItem extends NuclearWasteItem {

    public String fullName = "";
    protected boolean hasXenon = true;

    public RBMKPelletItem(Properties props, String fullName) {
        super(props);
        this.fullName = fullName;
    }

    public record Stage(int depletion, boolean xenon) {

        public static final Stage FRESH = new Stage(0, false);
        public static final Codec<Stage> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                Codec.intRange(0, 4)
                                                        .optionalFieldOf("depletion", 0)
                                                        .forGetter(Stage::depletion),
                                                Codec.BOOL
                                                        .optionalFieldOf("xenon", false)
                                                        .forGetter(Stage::xenon))
                                        .apply(i, Stage::new));
        public static final StreamCodec<ByteBuf, Stage> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        Stage::depletion,
                        ByteBufCodecs.BOOL,
                        Stage::xenon,
                        Stage::new);

        public static Stage ofLegacy(int meta) {
            return new Stage(meta % 5, meta >= 5);
        }
    }

    public RBMKPelletItem disableXenon() {
        this.hasXenon = false;
        return this;
    }

    public boolean isXenonEnabled() {
        return hasXenon;
    }

    public static Stage stage(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.RBMK_PELLET.get(), Stage.FRESH);
    }

    public ItemStack of(Stage stage) {
        ItemStack stack = new ItemStack(this);
        stack.set(ModDataComponents.RBMK_PELLET.get(), stage);
        return stack;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.literal(this.fullName).withStyle(ChatFormatting.ITALIC));
        adder.accept(
                Component.translatable("desc.item.rbmkPellet.pelletForRecycling")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));

        Stage stage = stage(stack);

        switch (stage.depletion()) {
            case 0 ->
                    adder.accept(
                            Component.translatable("desc.item.rbmkPellet.brandNew")
                                    .withStyle(ChatFormatting.GOLD));
            case 1 ->
                    adder.accept(
                            Component.translatable("desc.item.rbmkPellet.barelyDepleted")
                                    .withStyle(ChatFormatting.YELLOW));
            case 2 ->
                    adder.accept(
                            Component.translatable("desc.item.rbmkPellet.moderatelyDepleted")
                                    .withStyle(ChatFormatting.GREEN));
            case 3 ->
                    adder.accept(
                            Component.translatable("desc.item.rbmkPellet.highlyDepleted")
                                    .withStyle(ChatFormatting.DARK_GREEN));
            case 4 ->
                    adder.accept(
                            Component.translatable("desc.item.rbmkPellet.fullyDepleted")
                                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        if (stage.xenon())
            adder.accept(
                    Component.translatable("desc.item.rbmkPellet.highXenonPoison")
                            .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
