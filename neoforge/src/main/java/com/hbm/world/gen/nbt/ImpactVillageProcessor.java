/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.world.gen.nbt;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class ImpactVillageProcessor implements StructureProcessor {

    public static final MapCodec<ImpactVillageProcessor> MAP_CODEC =
            MapCodec.unit(() -> ImpactVillageProcessor.INSTANCE);
    public static final ImpactVillageProcessor INSTANCE = new ImpactVillageProcessor();

    private ImpactVillageProcessor() {}

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos targetPosition,
            BlockPos referencePos,
            BlockPos templateRelativePos,
            StructureTemplate.StructureBlockInfo processedBlockInfo,
            StructurePlaceSettings settings) {
        BlockState state = processedBlockInfo.state();
        if (state.isAir()) return processedBlockInfo;
        BlockState ruin;
        if (state.is(BlockTags.MINEABLE_WITH_AXE)
                || state.instrument() == NoteBlockInstrument.HAT
                || state.is(Blocks.LADDER)
                || state.getBlock() instanceof CropBlock
                || state.is(Blocks.CHEST)
                || state.getBlock() instanceof DoorBlock
                || state.is(BlockTags.WOOL)
                || state.is(BlockTags.BEDS)
                || state.is(Blocks.WATER)
                || singleStoneSlab(state)) {
            ruin = Blocks.AIR.defaultBlockState();
        } else if (state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.STONE_BRICKS)
                || state.is(Blocks.MOSSY_STONE_BRICKS)
                || state.is(Blocks.CRACKED_STONE_BRICKS)
                || state.is(Blocks.CHISELED_STONE_BRICKS)) {
            ruin =
                    roll(level, processedBlockInfo.pos())
                            ? Blocks.GRAVEL.defaultBlockState()
                            : state;
        } else if (state.is(Blocks.SANDSTONE)
                || state.is(Blocks.CHISELED_SANDSTONE)
                || state.is(Blocks.CUT_SANDSTONE)) {
            ruin = roll(level, processedBlockInfo.pos()) ? Blocks.SAND.defaultBlockState() : state;
        } else if (state.is(Blocks.FARMLAND)) {
            ruin = Blocks.DIRT.defaultBlockState();
        } else {
            return processedBlockInfo;
        }
        return ruin == state
                ? processedBlockInfo
                : new StructureTemplate.StructureBlockInfo(processedBlockInfo.pos(), ruin, null);
    }

    private static boolean singleStoneSlab(BlockState state) {
        return (state.is(Blocks.SMOOTH_STONE_SLAB)
                        || state.is(Blocks.SANDSTONE_SLAB)
                        || state.is(Blocks.PETRIFIED_OAK_SLAB)
                        || state.is(Blocks.COBBLESTONE_SLAB)
                        || state.is(Blocks.BRICK_SLAB)
                        || state.is(Blocks.STONE_BRICK_SLAB)
                        || state.is(Blocks.NETHER_BRICK_SLAB)
                        || state.is(Blocks.QUARTZ_SLAB))
                && state.getValue(SlabBlock.TYPE) != SlabType.DOUBLE;
    }

    private static boolean roll(LevelReader level, BlockPos pos) {
        return ProcessorRandom.bounded(level, pos, ProcessorRandom.IMPACT_VILLAGE, 3) == 1;
    }

    @Override
    public MapCodec<ImpactVillageProcessor> codec() {
        return MAP_CODEC;
    }
}
