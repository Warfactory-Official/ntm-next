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

import com.hbm.world.ImpactWorldgen;
import com.hbm.world.gen.nbt.ImpactVillageProcessor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SinglePoolElement.class)
public abstract class MixinSinglePoolElement {
    @WrapOperation(
            method = "place",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/levelgen/structure/pools/SinglePoolElement;getSettings(Lnet/minecraft/world/level/block/Rotation;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Lnet/minecraft/world/level/levelgen/structure/templatesystem/LiquidSettings;Z)Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;"))
    private StructurePlaceSettings hbm$impactVillageRuin(
            SinglePoolElement element,
            Rotation rotation,
            BoundingBox chunkBox,
            LiquidSettings liquidSettings,
            boolean keepJigsaws,
            Operation<StructurePlaceSettings> original) {
        StructurePlaceSettings settings =
                original.call(element, rotation, chunkBox, liquidSettings, keepJigsaws);
        if (ImpactWorldgen.VILLAGE_RUIN.isBound())
            settings.addProcessor(ImpactVillageProcessor.INSTANCE);
        return settings;
    }
}
