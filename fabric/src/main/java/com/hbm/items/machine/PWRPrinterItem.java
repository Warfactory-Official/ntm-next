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

import com.hbm.blockentity.machine.PWRControllerBlockEntity;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.PwrPrintLayout;
import com.hbm.util.ChunkUtil;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

public final class PWRPrinterItem extends Item {

    public PWRPrinterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(ChunkUtil.blockEntityIfLoaded(context.getLevel(), context.getClickedPos())
                instanceof PWRControllerBlockEntity)) {
            return InteractionResult.PASS;
        }
        if (context.getPlayer() instanceof ServerPlayer player) {
            PwrPrintData data = PwrPrintData.capture(player.level(), context.getClickedPos());
            if (data == null) {
                player.sendSystemMessage(Component.translatable("argument.pos.unloaded"));
                return InteractionResult.FAIL;
            }
            PacketWire.sendTo(
                    PwrPrintLayout.encode(data.min(), data.max(), data.direction(), data), player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("desc.item.pwrPrinter.use"));
    }
}
