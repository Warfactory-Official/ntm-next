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

package com.hbm.client.model;

import com.hbm.platform.Platform;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

final class SectionGeometryShaders {
    private static final boolean IRIS = Platform.isModLoaded("iris");

    private SectionGeometryShaders() {}

    static @Nullable ChunkSectionLayer layer(BlockState state) {
        return IRIS ? IrisLayer.layer(state) : null;
    }

    private static final class IrisLayer {
        static @Nullable ChunkSectionLayer layer(BlockState state) {
            var types = WorldRenderingSettings.INSTANCE.getBlockTypeIds();
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
