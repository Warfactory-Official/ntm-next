/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

import com.hbm.api.energymk2.PowerGraph;
import com.hbm.blockentity.network.PylonBaseBlockEntity;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.items.ModDataComponents;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class WiringItem extends Item {

    public WiringItem(Properties props) {
        super(props);
    }

    private static @Nullable PylonBaseBlockEntity pylonAt(Level level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        BlockPos target = core == null ? pos : core;
        return level.getBlockEntity(target) instanceof PylonBaseBlockEntity pylon ? pylon : null;
    }

    private static String connect(
            ServerLevel level, PylonBaseBlockEntity first, PylonBaseBlockEntity second) {
        return switch (PylonBaseBlockEntity.connectionResult(first, second)) {
            case 0 -> {
                PowerGraph.get(level)
                        .addRemoteLink(first.getBlockPos().asLong(), second.getBlockPos().asLong());
                first.refreshWireLinks();
                second.refreshWireLinks();
                yield "Wire end";
            }
            case 1 -> "Wire error - Pylons are not the same type";
            case 2 -> "Wire error - Cannot connect to the same pylon";
            default -> "Wire error - Pylon is too far away";
        };
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) return InteractionResult.PASS;
        Level level = context.getLevel();
        PylonBaseBlockEntity clicked = pylonAt(level, context.getClickedPos());
        if (clicked == null) return InteractionResult.PASS;
        ItemStack stack = context.getItemInHand();

        Long stored = stack.get(ModDataComponents.WIRE_TARGET.get());
        if (stored == null) {
            stack.set(ModDataComponents.WIRE_TARGET.get(), clicked.getBlockPos().asLong());
            if (!level.isClientSide())
                player.sendSystemMessage(Component.translatable("desc.item.wiring.wireStart"));
            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ServerLevel server = (ServerLevel) level;
        stack.remove(ModDataComponents.WIRE_TARGET.get());
        PylonBaseBlockEntity first = pylonAt(server, BlockPos.of(stored));
        player.sendSystemMessage(
                Component.literal(first == null ? "Wire error" : connect(server, first, clicked)));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        Long stored = stack.get(ModDataComponents.WIRE_TARGET.get());
        if (stored == null) {
            adder.accept(Component.translatable("desc.item.wiring.rightClickPolesTo"));
            return;
        }
        BlockPos pos = BlockPos.of(stored);
        adder.accept(Component.translatable("desc.item.wiring.wireStartX", pos.getX()));
        adder.accept(Component.translatable("desc.item.wiring.wireStartY", pos.getY()));
        adder.accept(Component.translatable("desc.item.wiring.wireStartZ", pos.getZ()));
    }
}
