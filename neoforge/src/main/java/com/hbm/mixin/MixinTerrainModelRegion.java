// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.model.SectionGeometry;
import com.hbm.interfaces.injected.IClientCoreHint;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RenderSectionRegion.class)
public class MixinTerrainModelRegion implements SectionGeometry.TerrainView, IClientCoreHint {
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
