// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.sodium;

import com.hbm.client.model.SectionGeometry;
import com.hbm.interfaces.injected.IClientCoreHint;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice", remap = false)
public class LevelSliceMixin implements SectionGeometry.TerrainView, IClientCoreHint {
    @Unique private @Nullable BlockPos hbm$coreHint;

    @Override
    public @Nullable BlockPos hbm$coreHint() {
        return hbm$coreHint;
    }

    @Override
    public void hbm$coreHint(@Nullable BlockPos pos) {
        hbm$coreHint = pos;
    }

    @ModifyExpressionValue(
            method = "prepare",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/chunk/LevelChunkSection;hasOnlyAir()Z"))
    private static boolean hbm$includeGeometry(
            boolean empty,
            @Local(argsOnly = true) Level level,
            @Local(argsOnly = true) SectionPos section) {
        return empty && !SectionGeometry.has(level, section.asLong());
    }
}
