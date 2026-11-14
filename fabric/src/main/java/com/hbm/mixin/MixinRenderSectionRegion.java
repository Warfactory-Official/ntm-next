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

package com.hbm.mixin;

import com.hbm.client.model.SectionGeometry;
import com.hbm.interfaces.injected.ClientCoreHintAccess;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RenderSectionRegion.class)
public class MixinRenderSectionRegion implements SectionGeometry.TerrainView, ClientCoreHintAccess {
    @Unique private @Nullable BlockPos hbm$coreHint;

    @Override
    public @Nullable BlockPos hbm$coreHint() {
        return hbm$coreHint;
    }

    @Override
    public void hbm$coreHint(@Nullable BlockPos pos) {
        hbm$coreHint = pos;
    }
}
