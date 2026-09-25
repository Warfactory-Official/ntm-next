// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.compat;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;

public interface NebBuffers {
    RegistryFriendlyByteBuf hbm$raw(RegistryAccess registries);

    RegistryFriendlyByteBuf hbm$packet(RegistryAccess registries);

    int[] hbm$sizes(int count);
}
