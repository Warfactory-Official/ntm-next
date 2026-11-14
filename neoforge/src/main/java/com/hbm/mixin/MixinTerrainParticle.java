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

import com.hbm.client.model.PaddedSpriteContents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TerrainParticle.class)
public abstract class MixinTerrainParticle {

    @Shadow @Final @Mutable private float uo;
    @Shadow @Final @Mutable private float vo;

    private static float hbm$clampOffset(float offset, float scale) {
        float max = Mth.clamp(4.0F * scale - 1.0F, 0.0F, 3.0F);
        return offset * (max / 3.0F);
    }

    @Inject(
            method =
                    "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDDLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
            at = @At("TAIL"))
    private void hbm$clampPaddedOffsets(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xa,
            double ya,
            double za,
            BlockState blockState,
            BlockPos pos,
            CallbackInfo ci) {
        var sprite =
                Minecraft.getInstance()
                        .getModelManager()
                        .getBlockStateModelSet()
                        .getParticleMaterial(blockState)
                        .sprite();
        if (sprite.contents() instanceof PaddedSpriteContents padded) {
            this.uo = hbm$clampOffset(this.uo, padded.uScale());
            this.vo = hbm$clampOffset(this.vo, padded.vScale());
        }
    }
}
