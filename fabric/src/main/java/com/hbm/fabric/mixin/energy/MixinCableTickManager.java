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

package com.hbm.fabric.mixin.energy;

import com.hbm.fabric.energy.TechRebornEnergy;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import techreborn.blockentity.cable.CableBlockEntity;
import techreborn.init.TRContent;

@Mixin(targets = "techreborn.blockentity.cable.CableTickManager", remap = false)
public abstract class MixinCableTickManager {
    @WrapMethod(method = "handleCableTick")
    private static void hbm$component(CableBlockEntity cable, Operation<Void> original) {
        original.call(cable);
        TechRebornEnergy.end();
    }

    @WrapOperation(
            method = "handleCableTick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ltechreborn/blockentity/cable/CableTickManager;gatherCables(Ltechreborn/blockentity/cable/CableBlockEntity;)V"))
    private static void hbm$begin(CableBlockEntity cable, Operation<Void> original) {
        original.call(cable);
        TechRebornEnergy.begin(cable);
    }

    @WrapOperation(
            method = "handleCableTick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ltechreborn/blockentity/cable/CableBlockEntity;appendTargets(Ljava/util/List;)V"))
    private static void hbm$member(
            CableBlockEntity cable, List<?> targets, Operation<Void> original) {
        original.call(cable, targets);
        TechRebornEnergy.member(cable);
    }

    @WrapOperation(
            method = "handleCableTick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ltechreborn/blockentity/cable/CableTickManager;dispatchTransfer(Ltechreborn/init/TRContent$Cables;Ltechreborn/blockentity/cable/CableTickManager$TransferOperation;J)J",
                            ordinal = 0))
    private static long hbm$pull(
            TRContent.Cables type,
            @Coerce Object operation,
            long maxAmount,
            Operation<Long> original) {
        TechRebornEnergy.dispatching(0);
        return original.call(type, operation, maxAmount);
    }

    @WrapOperation(
            method = "handleCableTick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ltechreborn/blockentity/cable/CableTickManager;dispatchTransfer(Ltechreborn/init/TRContent$Cables;Ltechreborn/blockentity/cable/CableTickManager$TransferOperation;J)J",
                            ordinal = 1))
    private static long hbm$push(
            TRContent.Cables type,
            @Coerce Object operation,
            long maxAmount,
            Operation<Long> original) {
        TechRebornEnergy.dispatching(1);
        return original.call(type, operation, maxAmount);
    }

    @Inject(
            method = "handleCableTick",
            at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
    private static void hbm$settled(
            CableBlockEntity cable,
            CallbackInfo ci,
            @Local(ordinal = 0) long capacity,
            @Local(ordinal = 1) long amount) {
        TechRebornEnergy.settled(amount, capacity);
    }
}
