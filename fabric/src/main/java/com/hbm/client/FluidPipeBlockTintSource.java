// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.api.fluidmk2.FluidPipeTintData;
import com.hbm.wiaj.WorldInAJar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;

public final class FluidPipeBlockTintSource implements BlockTintSource {

    public static final FluidPipeBlockTintSource INSTANCE = new FluidPipeBlockTintSource(false);
    public static final FluidPipeBlockTintSource LIGHTENED = new FluidPipeBlockTintSource(true);

    private final boolean lightened;

    private FluidPipeBlockTintSource(boolean lightened) {
        this.lightened = lightened;
    }

    @Override
    public int color(BlockState state) {
        return apply(FluidPipeTintData.DEFAULT_COLOR);
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        if (level instanceof WorldInAJar jar)
            return apply(FluidPipeTintData.colorFor(jar.pipeFluid(pos)));
        return apply(FluidPipeTintData.colorAt(Minecraft.getInstance().level.dimension(), pos));
    }

    @Override
    public int colorAsTerrainParticle(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return colorInWorld(state, level, pos);
    }

    private int apply(int argb) {
        if (!lightened) return argb;
        int r = ARGB.red(argb), g = ARGB.green(argb), b = ARGB.blue(argb);
        r = (int) (r + (255 - r) * 0.25);
        g = (int) (g + (255 - g) * 0.25);
        b = (int) (b + (255 - b) * 0.25);
        return ARGB.color(ARGB.alpha(argb), r, g, b);
    }
}
