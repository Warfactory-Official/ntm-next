// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.ntl;

import com.hbm.uninos.graph.IGraphProvider;
import com.mojang.serialization.Codec;
import java.util.List;

public final class PneumaticGraphProvider implements IGraphProvider<PneumaticData> {

    public static final PneumaticGraphProvider INSTANCE = new PneumaticGraphProvider();

    private PneumaticGraphProvider() {}

    @Override
    public boolean dataCompatible(PneumaticData a, PneumaticData b) {
        return true;
    }

    @Override
    public Codec<List<PneumaticData>> dataTableCodec() {
        return PneumaticData.CODEC.listOf();
    }
}
