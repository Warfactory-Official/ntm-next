// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.packet.toserver.RBMKDiagRequestPayload;
import com.hbm.platform.Services;
import com.hbm.util.I18nUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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

    public static void build(
            Level level, BlockPos pos, BlockState state, ILookOverlay.LookInfo info) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos, state);
        if (core == null) return;
        long now = System.nanoTime();
        if (!core.equals(requested) || now - requestedAt >= INTERVAL_NANOS) {
            requested = core.immutable();
            requestedAt = now;
            Services.NETWORK.sendToServer(new RBMKDiagRequestPayload(requested));
        }
        info.heading(I18nUtil.resolveKey("hud.hbm.dodd"), 0x00FF00, 0x006000, -20);
        info.title(state.getBlock().getName().getString(), 0xFFFF00, 0x606000);
        info.plainLines();
        if (shown == null || !core.equals(shownCore)) return;
        List<String> keys = new ArrayList<>(shown.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            String value = shown.get(key).toString();

            char last = value.isEmpty() ? 0 : value.charAt(value.length() - 1);
            if (last == 'd' || last == 's' || last == 'b')
                value = value.substring(0, value.length() - 1);
            info.line(I18nUtil.resolveKey("block.hbm.rbmk.dodd." + key) + ": " + value);
        }
    }

    public static void received(BlockPos core, CompoundTag diagnostics) {
        shownCore = core;
        shown = diagnostics;
    }
}
