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
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import team.reborn.energy.api.EnergyStorage;

@Mixin(targets = "techreborn.blockentity.cable.CableTickManager$SortableStorage", remap = false)
public abstract class MixinSortableStorage {
    @WrapOperation(
            method = "<init>",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ltechreborn/blockentity/cable/CableTickManager$TransferOperation;transfer(Lteam/reborn/energy/api/EnergyStorage;JLnet/fabricmc/fabric/api/transfer/v1/transaction/Transaction;)J"))
    private long hbm$simulated(
            @Coerce Object operation,
            EnergyStorage target,
            long maxAmount,
            Transaction transaction,
            Operation<Long> original) {
        long accepted = original.call(operation, target, maxAmount, transaction);
        TechRebornEnergy.simulated(target, accepted);
        return accepted;
    }
}
