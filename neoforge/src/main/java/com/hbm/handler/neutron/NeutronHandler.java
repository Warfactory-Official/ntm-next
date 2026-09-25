// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.neutron;

import com.hbm.handler.neutron.NeutronNodeWorld.StreamWorld;
import com.hbm.tileentity.machine.rbmk.RBMKConfig;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

public final class NeutronHandler {

    private static int ticks = 0;

    private NeutronHandler() {}

    public static void onServerTick(MinecraftServer server) {

        int cacheTime = 20;
        boolean cacheClear = ticks >= cacheTime;
        if (cacheClear) ticks = 0;
        ticks++;

        NeutronNodeWorld.removeEmptyWorlds();

        for (Map.Entry<Level, StreamWorld> entry : NeutronNodeWorld.streamWorlds.entrySet()) {
            Level world = entry.getKey();
            RBMKNeutronHandler.reflectorEfficiency = RBMKConfig.getReflectorEfficiency(world);
            RBMKNeutronHandler.absorberEfficiency = RBMKConfig.getAbsorberEfficiency(world);
            RBMKNeutronHandler.moderatorEfficiency = RBMKConfig.getModeratorEfficiency(world);

            RBMKNeutronHandler.columnHeight = RBMKConfig.getColumnHeight(world) + 1;
            RBMKNeutronHandler.fluxRange = RBMKConfig.getFluxRange(world);

            entry.getValue().runStreamInteractions(world);
            entry.getValue().removeAllStreams();

            if (cacheClear) entry.getValue().cleanNodes();
        }
    }
}
