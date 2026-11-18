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

import com.hbm.util.datafix.DataFixClock;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.util.worldupdate.RegionStorageUpgrader;
import net.minecraft.util.worldupdate.WorldUpgrader;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WorldUpgrader.class)
public abstract class MixinWorldUpgrader {
    @Shadow @Final private LevelStorageSource.LevelStorageAccess levelStorage;

    @ModifyExpressionValue(
            method =
                    "upgradeLevels(Lnet/minecraft/util/datafix/DataFixTypes;Lnet/minecraft/util/worldupdate/RegionStorageUpgrader$Builder;Ljava/util/function/BiFunction;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/util/worldupdate/RegionStorageUpgrader$Builder;setDataFixType(Lnet/minecraft/util/datafix/DataFixTypes;)Lnet/minecraft/util/worldupdate/RegionStorageUpgrader$Builder;"))
    private RegionStorageUpgrader.Builder hbm$datafixClock(RegionStorageUpgrader.Builder builder) {
        return builder.setDataFixContextTag(
                DataFixClock.savedContext(
                        levelStorage.getLevelPath(LevelResource.LEVEL_DATA_FILE)));
    }
}
