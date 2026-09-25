// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.hbm.uninos.graph.IGraphProvider;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.state.BlockState;

public final class FluidPipeGraphProvider implements IGraphProvider<PipeData> {

    public static final FluidPipeGraphProvider INSTANCE = new FluidPipeGraphProvider();

    private FluidPipeGraphProvider() {}

    @Override
    public PipeData createData(BlockState state) {
        return PipeData.UNSET;
    }

    @Override
    public boolean dataCompatible(PipeData a, PipeData b) {
        return a.equals(b);
    }

    @Override
    public Codec<PipeData> dataCodec() {
        return PipeData.CODEC;
    }
}
