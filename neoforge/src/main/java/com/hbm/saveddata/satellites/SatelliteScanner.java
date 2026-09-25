// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.saveddata.satellites;

import com.mojang.serialization.MapCodec;

public class SatelliteScanner extends Satellite {

    public static final MapCodec<SatelliteScanner> CODEC = MapCodec.unit(SatelliteScanner::new);

    public SatelliteScanner() {}

    @Override
    public SatelliteType type() {
        return SatelliteType.SCANNER;
    }
}
