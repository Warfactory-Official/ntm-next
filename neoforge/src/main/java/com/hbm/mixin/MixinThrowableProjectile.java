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

import com.hbm.entity.projectile.ProjectileEntity;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ThrowableProjectile.class)
public abstract class MixinThrowableProjectile extends Projectile {
    protected MixinThrowableProjectile(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    @WrapMethod(method = "tick")
    private void hbm$attachedOrInterpolated(Operation<Void> original) {
        if ((Object) this instanceof ProjectileEntity projectile && projectile.isFlightPaused()) {
            applyEffectsFromBlocks();
            super.tick();
            projectile.tickInterpolation();
        } else {
            original.call();
        }
    }

    @ModifyArg(
            method = "applyInertia",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"),
            index = 0)
    private double hbm$waterDrag(double drag) {
        return isInWater() && (Object) this instanceof ProjectileEntity projectile
                ? projectile.getWaterDrag()
                : drag;
    }

    @WrapOperation(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getHitResultOnMoveVector"
                                            + "(Lnet/minecraft/world/entity/Entity;Ljava/util/function/Predicate;)Lnet/minecraft/world/phys/HitResult;"))
    private HitResult hbm$trace(
            Entity source, Predicate<Entity> matching, Operation<HitResult> original) {
        var result =
                source instanceof ProjectileEntity projectile
                        ? projectile.traceFlight(matching)
                        : original.call(source, matching);
        if (result.getType() != HitResult.Type.MISS
                && !(source instanceof ProjectileEntity penetrating && penetrating.doesPenetrate())
                && EventHooks.onProjectileImpact((Projectile) source, result)) {
            Vec3 end = source.position().add(source.getDeltaMovement());
            return BlockHitResult.miss(end, Direction.UP, BlockPos.containing(end));
        }
        return result;
    }

    @WrapOperation(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/projectile/ThrowableProjectile;setPos(Lnet/minecraft/world/phys/Vec3;)V"))
    private void hbm$boxCollision(
            ThrowableProjectile self,
            Vec3 position,
            Operation<Void> original,
            @Local HitResult result) {
        if (result.getType() == HitResult.Type.MISS
                && self instanceof ProjectileEntity projectile
                && projectile.fullBlockCollisions()) {
            move(MoverType.SELF, getDeltaMovement());
        } else {
            original.call(self, position);
        }
    }

    @Redirect(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/neoforged/neoforge/event/EventHooks;onProjectileImpact"
                                            + "(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/world/phys/HitResult;)Z"))
    private boolean hbm$impactAlreadyChecked(Projectile projectile, HitResult result) {
        return false;
    }
}
