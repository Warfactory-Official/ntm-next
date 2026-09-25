// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
public class SectionRegionGeometryMixin implements SectionGeometry.Region {
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
