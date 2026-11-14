/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

package com.hbm.world.gen.component;

import com.hbm.util.RegistryUtil;
import com.hbm.world.structure.HbmStructureTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class StartingHubPiece extends NtmBranchingPiece {

    private static final int WIDTH = 7, HEIGHT = 5, DEPTH = 7;

    private boolean pathEast;
    private boolean pathAntiNormal;
    private boolean pathWest;
    private int hatchHeight;

    public StartingHubPiece(BlockPos origin, RandomSource random) {
        this(origin, Direction.from2DDataValue(random.nextInt(4)));
    }

    private StartingHubPiece(BlockPos origin, Direction direction) {
        super(
                HbmStructureTypes.NTM_BUNKER_STARTING_HUB.get(),
                0,
                StructurePiece.makeBoundingBox(
                        origin.getX(),
                        origin.getY(),
                        origin.getZ(),
                        direction,
                        WIDTH + 1,
                        HEIGHT + 1,
                        DEPTH + 1));
        this.setOrientation(direction);
    }

    public StartingHubPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_BUNKER_STARTING_HUB.get(), context, tag);
        this.pathEast = tag.getBooleanOr("PathEast", false);
        this.pathAntiNormal = tag.getBooleanOr("PathAntiNormal", false);
        this.pathWest = tag.getBooleanOr("PathWest", false);
        this.hatchHeight = tag.getIntOr("Hatch", 0);
    }

    private static BlockState stair(Direction facing, boolean top) {
        return hbmState("concrete_smooth_stairs")
                .setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }

    public void setHatchHeight(int hatchHeight) {
        this.hatchHeight = hatchHeight;
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putBoolean("PathEast", pathEast);
        tag.putBoolean("PathAntiNormal", pathAntiNormal);
        tag.putBoolean("PathWest", pathWest);
        tag.putInt("Hatch", hatchHeight);
    }

    @Override
    public void buildComponent(
            NtmBranchingStart start, StructurePiecesBuilder accessor, RandomSource random) {
        this.pathEast = this.generateChildEast(start, accessor, random, 5, 1) != null;
        this.pathAntiNormal = this.generateChildAntiNormal(start, accessor, random, 4, 1) != null;
        this.pathWest = this.generateChildWest(start, accessor, random, 3, 1) != null;
    }

    public static final Identifier TEMPLATE_ID = RegistryUtil.id("component/starting_hub");

    @Override
    protected Identifier templateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected void buildAfterTemplate(
            WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        if (this.pathEast) this.generateAirBox(level, chunkBB, 7, 1, 2, 7, 2, 3);
        if (this.pathAntiNormal) this.generateAirBox(level, chunkBB, 3, 1, 0, 4, 2, 0);
        if (this.pathWest) this.generateAirBox(level, chunkBB, 0, 1, 2, 0, 2, 3);
        BlockState reinforcedStone = hbmState("reinforced_stone");
        BlockState concreteSlab = hbmState("concrete_slab");
        int hpos = this.hatchHeight;

        this.placeBlock(level, concreteSlab, 0, hpos, 5, chunkBB);
        this.generateBox(
                level,
                chunkBB,
                1,
                hpos,
                4,
                1,
                hpos,
                6,
                stair(Direction.EAST, false),
                stair(Direction.EAST, false),
                false);
        this.placeBlock(level, concreteSlab, 2, hpos, 3, chunkBB);
        this.placeBlock(level, stair(Direction.SOUTH, false), 2, hpos, 4, chunkBB);
        this.placeBlock(
                level,
                hbmState("trapdoor_steel")
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                        .setValue(TrapDoorBlock.HALF, Half.TOP)
                        .setValue(TrapDoorBlock.OPEN, false),
                2,
                hpos,
                5,
                chunkBB);
        this.placeBlock(level, stair(Direction.NORTH, false), 2, hpos, 6, chunkBB);
        this.placeBlock(level, concreteSlab, 2, hpos, 7, chunkBB);
        this.generateBox(
                level,
                chunkBB,
                3,
                hpos,
                4,
                3,
                hpos,
                6,
                stair(Direction.WEST, false),
                stair(Direction.WEST, false),
                false);
        this.placeBlock(level, concreteSlab, 4, hpos, 5, chunkBB);

        this.generateBox(
                level, chunkBB, 1, 6, 4, 1, hpos - 1, 6, reinforcedStone, reinforcedStone, false);
        this.generateBox(
                level, chunkBB, 2, 1, 6, 2, hpos - 1, 6, reinforcedStone, reinforcedStone, false);
        this.generateBox(
                level, chunkBB, 3, 6, 4, 3, hpos - 1, 6, reinforcedStone, reinforcedStone, false);
        this.generateBox(
                level, chunkBB, 2, 6, 4, 2, hpos - 1, 4, reinforcedStone, reinforcedStone, false);
        this.generateBox(
                level,
                chunkBB,
                2,
                1,
                5,
                2,
                hpos - 1,
                5,
                hbmState("ladder_sturdy").setValue(LadderBlock.FACING, Direction.NORTH),
                hbmState("ladder_sturdy").setValue(LadderBlock.FACING, Direction.NORTH),
                false);
    }
}
