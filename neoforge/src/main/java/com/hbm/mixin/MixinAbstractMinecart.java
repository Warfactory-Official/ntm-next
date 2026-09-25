// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.blocks.IMinecartRail;
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
        IMinecartRail rail = hbm$railUnder();
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
        IMinecartRail rail = hbm$railUnder();
        if (rail != null) rail.onMinecartPass(self);
    }

    @Unique
    private @Nullable IMinecartRail hbm$railUnder() {
        AbstractMinecart self = (AbstractMinecart) (Object) this;
        Level level = self.level();
        return level.getBlockState(self.getCurrentBlockPosOrRailBelow()).getBlock()
                        instanceof IMinecartRail rail
                ? rail
                : null;
    }
}
