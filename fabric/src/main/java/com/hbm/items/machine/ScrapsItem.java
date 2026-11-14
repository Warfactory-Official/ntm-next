/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.items.machine;

import com.hbm.client.ModifierKeys;
import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.MaterialRegistry;
import com.hbm.inventory.material.MaterialStack;
import com.hbm.inventory.material.MaterialTooltip;
import com.hbm.inventory.material.SmeltingBehavior;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
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
import org.jspecify.annotations.Nullable;

public class ScrapsItem extends Item {

    public ScrapsItem(Properties properties) {
        super(properties);
    }

    public static @Nullable MaterialStack getMats(ItemStack stack) {
        ScrapData data = data(stack);
        Material mat = data == null ? null : MaterialRegistry.byId(data.material());
        return mat == null ? null : new MaterialStack(mat, data.amount());
    }

    public static @Nullable Material material(ItemStack stack) {
        ScrapData data = data(stack);
        return data == null ? null : MaterialRegistry.byId(data.material());
    }

    private static @Nullable ScrapData data(ItemStack stack) {
        return stack.getItem() instanceof ScrapsItem
                ? stack.get(ModDataComponents.SCRAP.get())
                : null;
    }

    public static ItemStack create(MaterialStack stack) {
        return create(stack, false);
    }

    public static ItemStack create(MaterialStack stack, boolean liquid) {
        ItemStack scrap = new ItemStack(ModItems.SCRAPS);
        scrap.set(
                ModDataComponents.SCRAP.get(),
                new ScrapData(stack.material.id(), stack.amount, liquid));
        return scrap;
    }

    public static boolean isLiquid(ItemStack stack) {
        ScrapData data = stack.get(ModDataComponents.SCRAP.get());
        return data != null && data.liquid();
    }

    @Override
    public Component getName(ItemStack stack) {
        Material material = material(stack);
        if (material != null) {
            return isLiquid(stack)
                    ? material.displayName()
                    : Component.translatable("item.hbm.scraps", material.displayName());
        }
        return Component.translatable("desc.item.scraps.foundryScraps");
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        ScrapData data = data(stack);
        Material material = data == null ? null : MaterialRegistry.byId(data.material());
        if (material != null) {
            adder.accept(
                    Component.empty()
                            .append(MaterialTooltip.of(data.amount(), ModifierKeys.detailsHeld())));

            if (data.liquid() && material.behaviour() == SmeltingBehavior.ADDITIVE) {
                adder.accept(
                        Component.translatable("desc.item.scraps.additiveNotCastable")
                                .withStyle(ChatFormatting.DARK_RED));
            }
        }
    }

    public record ScrapData(int material, int amount, boolean liquid) {

        public static final Codec<ScrapData> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                Codec.INT
                                                        .fieldOf("material")
                                                        .forGetter(ScrapData::material),
                                                Codec.INT
                                                        .fieldOf("amount")
                                                        .forGetter(ScrapData::amount),
                                                Codec.BOOL
                                                        .optionalFieldOf("liquid", false)
                                                        .forGetter(ScrapData::liquid))
                                        .apply(i, ScrapData::new));

        public static final StreamCodec<ByteBuf, ScrapData> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        ScrapData::material,
                        ByteBufCodecs.VAR_INT,
                        ScrapData::amount,
                        ByteBufCodecs.BOOL,
                        ScrapData::liquid,
                        ScrapData::new);
    }
}
