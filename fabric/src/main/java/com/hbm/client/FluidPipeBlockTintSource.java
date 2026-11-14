/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client;

import com.hbm.api.fluidmk2.FluidPipeTintData;
import com.hbm.wiaj.WorldInAJar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
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
        return apply(FluidPipeTintData.colorAt(Minecraft.getInstance().level, pos));
    }

    @Override
    public int colorAsTerrainParticle(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return colorInWorld(state, level, pos);
    }

    private int apply(int argb) {
        return lightened ? FluidPipeTintData.lightened(argb) : argb;
    }
}
