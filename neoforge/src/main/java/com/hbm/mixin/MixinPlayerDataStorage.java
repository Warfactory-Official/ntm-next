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

import com.hbm.util.datafix.HbmDataFixers;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerDataStorage.class)
public abstract class MixinPlayerDataStorage {

    @ModifyReturnValue(
            method = "load(Lnet/minecraft/server/players/NameAndId;)Ljava/util/Optional;",
            at = @At("RETURN"))
    private Optional<CompoundTag> hbm$upgrade(Optional<CompoundTag> tag) {
        return tag.map(HbmDataFixers::upgradePlayer);
    }

    @ModifyExpressionValue(
            method = "save",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/storage/TagValueOutput;buildResult()Lnet/minecraft/nbt/CompoundTag;"))
    private CompoundTag hbm$stamp(CompoundTag tag) {
        HbmDataFixers.stamp(tag);
        return tag;
    }
}
