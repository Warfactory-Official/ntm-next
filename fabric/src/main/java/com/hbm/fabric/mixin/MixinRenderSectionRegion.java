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

package com.hbm.fabric.mixin;

import com.hbm.client.model.SectionGeometry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCopy;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSectionRegion.class)
public class MixinRenderSectionRegion implements SectionGeometry.Region {
    @Unique private SectionGeometry.Snapshot hbm$geometry;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void hbm$capture(
            ClientLevel level,
            int minX,
            int minY,
            int minZ,
            SectionCopy[] sections,
            CallbackInfo ci) {
        hbm$geometry =
                SectionGeometry.capture(
                        level, new BlockPos((minX + 1) << 4, (minY + 1) << 4, (minZ + 1) << 4));
    }

    @Override
    public SectionGeometry.Snapshot hbm$sectionGeometry() {
        return hbm$geometry;
    }
}
