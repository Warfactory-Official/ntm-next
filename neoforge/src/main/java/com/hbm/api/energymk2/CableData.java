// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.energymk2;

import com.mojang.serialization.Codec;

public record CableData() {

    public static final CableData INSTANCE = new CableData();

    public static final Codec<CableData> CODEC = Codec.BYTE.xmap(b -> INSTANCE, d -> (byte) 0);
}
