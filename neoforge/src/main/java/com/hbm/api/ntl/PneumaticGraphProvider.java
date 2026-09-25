// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.ntl;

import com.hbm.uninos.graph.IGraphProvider;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.state.BlockState;

public final class PneumaticGraphProvider implements IGraphProvider<PneumaticData> {

    public static final PneumaticGraphProvider INSTANCE = new PneumaticGraphProvider();

    private PneumaticGraphProvider() {}

    @Override
    public PneumaticData createData(BlockState state) {
        return PneumaticData.INSTANCE;
    }

    @Override
    public boolean dataCompatible(PneumaticData a, PneumaticData b) {
        return true;
    }

    @Override
    public Codec<PneumaticData> dataCodec() {
        return PneumaticData.CODEC;
    }
}
