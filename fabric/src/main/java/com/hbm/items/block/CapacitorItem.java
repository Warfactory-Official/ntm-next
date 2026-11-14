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

package com.hbm.items.block;

import com.hbm.blocks.machine.storage.CapacitorBlock;
import com.hbm.client.ModifierKeys;
import com.hbm.items.PersistentInfoBlockItem;
import com.hbm.util.NumberFormatter;
import com.hbm.util.TooltipStyle;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public final class CapacitorItem extends PersistentInfoBlockItem {

    public CapacitorItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        long ceiling = ((CapacitorBlock) getBlock()).maxPower;
        adder.accept(
                Component.translatable(
                                "desc.shared.storesUpTo", NumberFormatter.formatCompact(ceiling))
                        .withStyle(ChatFormatting.GOLD));
        adder.accept(
                Component.translatable(
                                "desc.shared.chargeSpeed",
                                NumberFormatter.formatCompact(ceiling / 200))
                        .withStyle(ChatFormatting.GOLD));
        adder.accept(
                Component.translatable(
                                "desc.shared.dischargeSpeed",
                                NumberFormatter.formatCompact(ceiling / 600))
                        .withStyle(ChatFormatting.GOLD));
        if ((FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT)
                && ModifierKeys.detailsHeld()) {
            TooltipStyle.lines(
                            Component.translatable("desc.hbm.capacitor")
                                    .withStyle(ChatFormatting.YELLOW))
                    .forEach(adder);
        } else {
            adder.accept(
                    TooltipStyle.holdDetails()
                            .copy()
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        super.appendHoverText(stack, context, display, adder, flag);
    }
}
