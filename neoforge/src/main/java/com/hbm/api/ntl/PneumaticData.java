// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.ntl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

public record PneumaticData() {

    public static final PneumaticData INSTANCE = new PneumaticData();
    public static final Codec<PneumaticData> CODEC = MapCodec.unit(INSTANCE).codec();
}
