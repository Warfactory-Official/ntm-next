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

package com.hbm.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.entity.living.CorrodentEntity;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(CorrodentEntity.class)
public abstract class MixinCorrodentEntity extends Monster {

    @Shadow private Vec3 surfacePosition;

    protected MixinCorrodentEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @ModifyArg(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/core/particles/BlockParticleOption;<init>(Lnet/minecraft/core/particles/ParticleType;Lnet/minecraft/world/level/block/state/BlockState;)V",
                            ordinal = 0),
            index = 1)
    private BlockState hbm$headDustState(BlockState state) {
        return hbm$surfaceDustState(state);
    }

    @ModifyArg(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/core/particles/BlockParticleOption;<init>(Lnet/minecraft/core/particles/ParticleType;Lnet/minecraft/world/level/block/state/BlockState;)V",
                            ordinal = 1),
            index = 1)
    private BlockState hbm$trailDustState(BlockState state) {
        return hbm$surfaceDustState(state);
    }

    private BlockState hbm$surfaceDustState(BlockState state) {
        return MultiblockSurface.particleStateOrAir(
                level(), BlockPos.containing(surfacePosition).below(), state);
    }

    @ModifyArg(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/core/particles/BlockParticleOption;<init>(Lnet/minecraft/core/particles/ParticleType;Lnet/minecraft/world/level/block/state/BlockState;)V",
                            ordinal = 2),
            index = 1)
    private BlockState hbm$footDustState(BlockState state) {
        return MultiblockSurface.particleStateOrAir(level(), getOnPos(), state);
    }

    @ModifyArg(
            method = "handleEntityEvent",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/core/particles/BlockParticleOption;<init>(Lnet/minecraft/core/particles/ParticleType;Lnet/minecraft/world/level/block/state/BlockState;)V"),
            index = 1)
    private BlockState hbm$emergenceDustState(
            BlockState state, @Local(name = "ground") BlockPos ground) {
        return MultiblockSurface.particleStateOrAir(level(), ground, state);
    }
}
