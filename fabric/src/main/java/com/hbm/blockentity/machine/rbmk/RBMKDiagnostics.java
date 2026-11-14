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

package com.hbm.blockentity.machine.rbmk;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.LookOverlay;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.RBMKDiagnosticsLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class RBMKDiagnostics {

    private static final long INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(50);
    private static @Nullable BlockPos requested;
    private static long requestedAt;
    private static @Nullable BlockPos shownCore;
    private static @Nullable CompoundTag shown;

    private RBMKDiagnostics() {}

    public static void build(Level level, BlockPos pos, BlockState state, LookOverlay info) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos, state);
        if (core == null) return;
        long now = System.nanoTime();
        if (!core.equals(requested) || now - requestedAt >= INTERVAL_NANOS) {
            requested = core.immutable();
            requestedAt = now;
            PacketWire.sendToServer(RBMKDiagnosticsLayout.encode(requested));
        }
        info.heading(
                Component.empty().append(Component.translatable("hud.hbm.dodd")),
                0x00FF00,
                0x006000,
                -20);
        info.title(state.getBlock().getName(), 0xFFFF00, 0x606000);
        info.plainRows();
        if (shown == null || !core.equals(shownCore)) return;
        List<String> keys = new ArrayList<>(shown.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            String value = shown.get(key).toString();
            char last = value.isEmpty() ? 0 : value.charAt(value.length() - 1);
            if (last == 'd' || last == 's' || last == 'b')
                value = value.substring(0, value.length() - 1);
            info.row(
                    Component.empty()
                            .append(Component.translatable("block.hbm.rbmk.dodd." + key))
                            .append(Component.literal(": " + value)));
        }
    }

    public static void received(BlockPos core, CompoundTag diagnostics) {
        shownCore = core;
        shown = diagnostics;
    }
}
