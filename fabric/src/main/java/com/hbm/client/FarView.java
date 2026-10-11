// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.packet.toserver.FarViewPayload;
import dev.engine_room.flywheel.api.lod.LodTerrain;
import net.minecraft.client.Minecraft;

public final class FarView {
    private FarView() {}

    public static void init() {
        LodTerrain.onChange(
                blocks -> {
                    if (Minecraft.getInstance().getConnection() != null)
                        FarViewPayload.report(blocks);
                });
    }

    public static void joined() {
        FarViewPayload.report(LodTerrain.distance());
    }
}
