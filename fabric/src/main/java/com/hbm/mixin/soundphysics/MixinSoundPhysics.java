// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.soundphysics;

import com.hbm.client.compat.SoundPhysicsOcclusion;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.sonicether.soundphysics.SoundPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SoundPhysics.class, remap = false)
public abstract class MixinSoundPhysics {
    @WrapMethod(method = "evaluateEnvironment")
    private static @Nullable Vec3 hbm$emitterContext(
            int source,
            double x,
            double y,
            double z,
            SoundSource category,
            Identifier sound,
            boolean auxOnly,
            Operation<Vec3> original) {
        var previous = SoundPhysicsOcclusion.begin(source);
        try {
            return original.call(source, x, y, z, category, sound, auxOnly);
        } finally {
            SoundPhysicsOcclusion.end(previous);
        }
    }

    @WrapOperation(
            method = "runOcclusion",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/sonicether/soundphysics/utils/RaycastUtils;rayCast(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private static BlockHitResult hbm$directRay(
            BlockGetter level,
            Vec3 from,
            Vec3 to,
            @Nullable BlockPos ignore,
            Operation<BlockHitResult> original,
            @Local(ordinal = 0) int step) {
        return step == 0
                ? SoundPhysicsOcclusion.initialRay(level, from, to, ignore, original)
                : original.call(level, from, to, ignore);
    }

    @WrapOperation(
            method = "evaluateEnvironment",
            at =
                    @At(
                            value = "INVOKE",
                            ordinal = 0,
                            target =
                                    "Lcom/sonicether/soundphysics/utils/RaycastUtils;rayCast(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private static BlockHitResult hbm$firstReflection(
            BlockGetter level,
            Vec3 from,
            Vec3 to,
            @Nullable BlockPos ignore,
            Operation<BlockHitResult> original) {
        return SoundPhysicsOcclusion.initialRay(level, from, to, ignore, original);
    }

    @WrapOperation(
            method =
                    "getSharedAirspace(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/sonicether/soundphysics/utils/RaycastUtils;rayCast(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private static BlockHitResult hbm$directAirspace(
            BlockGetter level,
            Vec3 from,
            Vec3 to,
            @Nullable BlockPos ignore,
            Operation<BlockHitResult> original) {
        return SoundPhysicsOcclusion.isOrigin(from)
                ? SoundPhysicsOcclusion.initialRay(level, from, to, ignore, original)
                : original.call(level, from, to, ignore);
    }
}
