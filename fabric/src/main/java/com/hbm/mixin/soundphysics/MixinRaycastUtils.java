// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.soundphysics;

import com.hbm.client.compat.SoundPhysicsOcclusion;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sonicether.soundphysics.utils.RaycastUtils;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RaycastUtils.class, remap = false)
public abstract class MixinRaycastUtils {
    @WrapOperation(
            method = "rayCast",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/BlockGetter;traverseBlocks(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Ljava/lang/Object;Ljava/util/function/BiFunction;Ljava/util/function/Function;)Ljava/lang/Object;"))
    private static Object hbm$emitterTraversal(
            Vec3 from,
            Vec3 to,
            Object context,
            BiFunction<Object, BlockPos, Object> visit,
            Function<Object, Object> miss,
            Operation<Object> original) {
        return SoundPhysicsOcclusion.traverse(from, to, context, visit, miss, original);
    }
}
