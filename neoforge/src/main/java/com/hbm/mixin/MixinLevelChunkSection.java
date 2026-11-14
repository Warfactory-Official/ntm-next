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

import com.hbm.blocks.ClimbBoxes;
import com.hbm.interfaces.injected.ClimbBoxSectionAccess;
import com.hbm.util.SectionGeneration;
import com.hbm.util.SectionMetadata;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunkSection.class)
public abstract class MixinLevelChunkSection implements ClimbBoxSectionAccess {
    @Unique public long hbm$generation;
    @Unique private short hbm$climbBoxes;

    @Override
    public int hbm$climbBoxes() {
        return hbm$climbBoxes;
    }

    @Inject(
            method =
                    "<init>(Lnet/minecraft/world/level/chunk/PalettedContainer;Lnet/minecraft/world/level/chunk/PalettedContainerRO;)V",
            at = @At("RETURN"))
    private void hbm$countLoaded(CallbackInfo ci) {
        hbm$recountClimbBoxes();
    }

    @Inject(
            method = "<init>(Lnet/minecraft/world/level/chunk/LevelChunkSection;)V",
            at = @At("RETURN"))
    private void hbm$copyClimbBoxes(LevelChunkSection source, CallbackInfo ci) {
        hbm$climbBoxes =
                (short)
                        ((com.hbm.interfaces.injected.ClimbBoxSectionAccess) (source))
                                .hbm$climbBoxes();
    }

    @Inject(method = "copy", at = @At("RETURN"))
    private void hbm$copyMetadata(CallbackInfoReturnable<LevelChunkSection> ci) {
        SectionMetadata.copy((LevelChunkSection) (Object) this, ci.getReturnValue());
    }

    @ModifyReturnValue(
            method =
                    "setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("RETURN"))
    private BlockState hbm$blockChanged(
            BlockState previous, int x, int y, int z, BlockState state, boolean checked) {
        if (previous != state) {
            SectionGeneration.changed((LevelChunkSection) (Object) this);
            if (ClimbBoxes.counts(previous)) hbm$climbBoxes--;
            if (ClimbBoxes.counts(state)) hbm$climbBoxes++;
        }
        return previous;
    }

    @Inject(
            method = {"read", "readBiomes", "fillBiomesFromNoise"},
            at = @At("RETURN"))
    private void hbm$paletteChanged(CallbackInfo ci) {
        SectionGeneration.changed((LevelChunkSection) (Object) this);
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void hbm$countRead(CallbackInfo ci) {
        hbm$recountClimbBoxes();
    }

    @Unique
    private void hbm$recountClimbBoxes() {
        PalettedContainer<BlockState> states = ((LevelChunkSection) (Object) this).getStates();
        if (!states.maybeHas(ClimbBoxes::counts)) {
            hbm$climbBoxes = 0;
            return;
        }
        int[] total = {0};
        states.count(
                (state, count) -> {
                    if (ClimbBoxes.counts(state)) total[0] += count;
                });
        hbm$climbBoxes = (short) total[0];
    }
}
