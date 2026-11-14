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

import com.hbm.api.environment.Environment;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AbstractFurnaceBlockEntity.class, priority = 500)
public abstract class MixinAbstractFurnaceBlockEntity {
    @WrapOperation(
            method = "serverTick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;getBurnDuration("
                                            + "Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)I"))
    private static int hbm$burnNeedsAir(
            AbstractFurnaceBlockEntity entity,
            ServerLevel fuelLevel,
            ItemStack fuel,
            Operation<Integer> original,
            ServerLevel level,
            BlockPos pos) {
        int burnTime = original.call(entity, fuelLevel, fuel);
        return burnTime > 0 && !fuel.is(Items.LAVA_BUCKET) && !Environment.breatheAir(level, pos, 0)
                ? 0
                : burnTime;
    }
}
