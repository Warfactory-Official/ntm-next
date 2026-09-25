// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.platform.Services;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

final class SectionGeometryShaders {
    private static final boolean IRIS = Services.PLATFORM.isModLoaded("iris");

    private SectionGeometryShaders() {}

    static @Nullable ChunkSectionLayer layer(BlockState state) {
        return IRIS ? IrisLayer.layer(state) : null;
    }

    private static final class IrisLayer {
        static @Nullable ChunkSectionLayer layer(BlockState state) {
            var types =
                    net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings.INSTANCE
                            .getBlockTypeIds();
            if (types == null) return null;
            var type = types.get(state.getBlock());
            if (type == null) return null;
            return switch (type) {
                case SOLID -> ChunkSectionLayer.SOLID;
                case CUTOUT, CUTOUT_MIPPED -> ChunkSectionLayer.CUTOUT;
                case TRANSLUCENT -> ChunkSectionLayer.TRANSLUCENT;
            };
        }
    }
}
