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

import com.hbm.world.gen.nbt.LegacyJigsawAssembler;
import com.hbm.world.gen.nbt.LegacyStartPoolElement;
import com.hbm.world.gen.nbt.ParentOverlapPoolElement;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(JigsawPlacement.class)
public abstract class MixinJigsawPlacement {
    @WrapOperation(
            method = "lambda$addPieces$2",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/levelgen/structure/pools/JigsawPlacement;addPieces(Lnet/minecraft/world/level/levelgen/RandomState;IZLnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;Lnet/minecraft/world/level/LevelHeightAccessor;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/Registry;Lnet/minecraft/world/level/levelgen/structure/PoolElementStructurePiece;Ljava/util/List;Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/level/levelgen/structure/pools/alias/PoolAliasLookup;Lnet/minecraft/world/level/levelgen/structure/templatesystem/LiquidSettings;)V"))
    private static void hbm$assembleDeclaredLegacy(
            RandomState randomState,
            int maxDepth,
            boolean expansion,
            ChunkGenerator generator,
            StructureTemplateManager templates,
            LevelHeightAccessor height,
            RandomSource random,
            Registry<StructureTemplatePool> pools,
            PoolElementStructurePiece start,
            List<PoolElementStructurePiece> pieces,
            VoxelShape free,
            PoolAliasLookup aliases,
            LiquidSettings liquid,
            Operation<Void> original,
            @Local(name = "aabb") AABB bounds) {
        if (start.getElement() instanceof LegacyStartPoolElement
                || start.getElement() instanceof ParentOverlapPoolElement) {
            LegacyJigsawAssembler.assemble(
                    randomState,
                    maxDepth,
                    expansion,
                    generator,
                    templates,
                    height,
                    random,
                    pools,
                    start,
                    pieces,
                    bounds,
                    aliases,
                    liquid);
        } else {
            original.call(
                    randomState,
                    maxDepth,
                    expansion,
                    generator,
                    templates,
                    height,
                    random,
                    pools,
                    start,
                    pieces,
                    free,
                    aliases,
                    liquid);
        }
    }
}
