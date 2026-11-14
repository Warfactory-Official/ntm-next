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

import com.hbm.blocks.MinecartRail;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractMinecart.class)
public class MixinAbstractMinecart {
    @ModifyReturnValue(method = "getMaxSpeed", at = @At("RETURN"))
    private double hbm$railMaxSpeed(double original) {
        MinecartRail rail = hbm$railUnder();
        return rail == null ? original : original * (rail.railMaxSpeed() / 0.4F);
    }

    @Inject(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/vehicle/minecart/MinecartBehavior;tick()V",
                            shift = At.Shift.AFTER))
    private void hbm$railPass(CallbackInfo ci) {
        AbstractMinecart self = (AbstractMinecart) (Object) this;
        if (!(self.level() instanceof ServerLevel)) return;
        MinecartRail rail = hbm$railUnder();
        if (rail != null) rail.onMinecartPass(self);
    }

    @Unique
    private @Nullable MinecartRail hbm$railUnder() {
        AbstractMinecart self = (AbstractMinecart) (Object) this;
        Level level = self.level();
        return level.getBlockState(self.getCurrentBlockPosOrRailBelow()).getBlock()
                        instanceof MinecartRail rail
                ? rail
                : null;
    }
}
