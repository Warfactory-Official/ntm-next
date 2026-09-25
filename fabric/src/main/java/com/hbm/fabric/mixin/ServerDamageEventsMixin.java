// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin;

import com.hbm.interfaces.NtmDamageContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.function.Function;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AllowDamage;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ServerLivingEntityEvents.class, remap = false)
abstract class ServerDamageEventsMixin {

    @WrapOperation(
            method = "<clinit>",
            at =
                    @At(
                            value = "INVOKE",
                            ordinal = 0,
                            target =
                                    "Lnet/fabricmc/fabric/api/event/EventFactory;createArrayBacked(Ljava/lang/Class;Ljava/util/function/Function;)Lnet/fabricmc/fabric/api/event/Event;"))
    private static Event<AllowDamage> hbm$earlyCancellation(
            Class<AllowDamage> type,
            Function<AllowDamage[], AllowDamage> factory,
            Operation<Event<AllowDamage>> original) {
        assert type == AllowDamage.class;
        Function<AllowDamage[], AllowDamage> wrapped =
                callbacks -> {
                    AllowDamage delegate = factory.apply(callbacks);
                    return (entity, source, amount) ->
                            delegate.allowDamage(entity, source, amount)
                                    || (((NtmDamageContext) entity).hbm$ignoreEarlyCancellation()
                                            && !entity.isDeadOrDying());
                };
        return original.call(type, wrapped);
    }
}
