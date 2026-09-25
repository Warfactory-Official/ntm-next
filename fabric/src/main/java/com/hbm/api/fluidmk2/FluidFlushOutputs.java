// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

public final class FluidFlushOutputs {

    private @Nullable FlushRegister register;
    private boolean adapted;

    public void provide(ServerLevel level, FluidFlushSender machine) {
        FlushRegister held = register;
        if (held == null) {
            register = held = new FlushRegister(machine);
            adapted = FlushRegister.adaptsSendingTanks(machine.getClass());
        }
        if (adapted) held.expectLanes(machine.getSendingTanks().length);
        held.flush(level);
    }

    public void invalidate() {
        if (register != null) register.invalidate();
    }
}
