// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
