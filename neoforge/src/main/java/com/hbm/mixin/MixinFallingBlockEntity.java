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

import com.hbm.entity.item.FallingBlockNTEntity;
import com.hbm.interfaces.StoredItems;
import com.hbm.items.special.CarriedBlockItems;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FallingBlockEntity.class)
public abstract class MixinFallingBlockEntity implements StoredItems {
    @ModifyExpressionValue(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/FallingBlock;isFree(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean hbm$ntmLanding(boolean original) {
        return !((Object) this instanceof FallingBlockNTEntity) && original;
    }

    @Shadow public @Nullable CompoundTag blockData;

    @Unique private @Nullable CompoundTag hbm$resolvedData;

    @Unique private @Nullable BlockEntity hbm$carriedInventory;

    @Override
    public void visitStoredItems(Visitor visitor) {
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;
        if (blockData == null
                || !self.getBlockState().hasBlockEntity()
                || !(self.level() instanceof ServerLevel server)) return;
        if (hbm$resolvedData != blockData) {
            hbm$carriedInventory =
                    CarriedBlockItems.load(
                            self.getBlockState(),
                            self.blockPosition(),
                            blockData,
                            server.registryAccess());
            hbm$resolvedData = blockData;
        }
        if (hbm$carriedInventory != null && visitor.visit(hbm$carriedInventory)) {
            blockData = hbm$carriedInventory.saveWithoutMetadata(server.registryAccess());
            hbm$resolvedData = blockData;
        }
    }
}
