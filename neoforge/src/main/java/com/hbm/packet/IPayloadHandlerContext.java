// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet;

import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

public interface IPayloadHandlerContext {

    Player player();

    default @Nullable Player playerOrNull() {
        try {
            return player();
        } catch (UnsupportedOperationException noPlayerYet) {
            return null;
        }
    }
}
