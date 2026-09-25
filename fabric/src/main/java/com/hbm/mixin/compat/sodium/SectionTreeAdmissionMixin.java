// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.sodium;

import com.hbm.client.model.SectionGeometry;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(
        targets = "net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager",
        remap = false)
public class SectionTreeAdmissionMixin {
    @Shadow @Final private ClientLevel level;

    @ModifyExpressionValue(
            method = "onSectionAdded",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/chunk/LevelChunkSection;hasOnlyAir()Z"))
    private boolean hbm$includeGeometry(boolean empty, int x, int y, int z) {
        return empty && !SectionGeometry.has(level, SectionPos.asLong(x, y, z));
    }
}
