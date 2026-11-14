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

package com.hbm.mixin.sodium;

import com.hbm.client.model.SectionGeometry;
import com.hbm.interfaces.injected.ClientCoreHintAccess;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LevelSlice.class, remap = false)
public class MixinLevelSlice implements SectionGeometry.TerrainView, ClientCoreHintAccess {
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
