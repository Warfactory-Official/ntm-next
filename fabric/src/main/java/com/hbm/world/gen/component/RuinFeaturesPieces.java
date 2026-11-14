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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public final class RuinFeaturesPieces {

    private RuinFeaturesPieces() {}

    public static class NTMRuin1Piece extends NtmComponentPiece {
        private static final int WIDTH = 8, HEIGHT = 6, DEPTH = 10;

        public NTMRuin1Piece(BlockPos origin, RandomSource random) {
            this(origin, getRandomHorizontalDirection(random));
        }

        private NTMRuin1Piece(BlockPos origin, Direction direction) {
            super(
                    pieceType(),
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

        public NTMRuin1Piece(StructurePieceSerializationContext context, CompoundTag tag) {
            super(pieceType(), context, tag);
        }

        private static StructurePieceType pieceType() {
            return HbmStructureTypes.NTM_RUIN1_PIECE.get();
        }

        @Override
        protected void addAdditionalSaveData(
                StructurePieceSerializationContext context, CompoundTag tag) {}

        public static final Identifier TEMPLATE_ID = RegistryUtil.id("component/ruin1");

        @Override
        protected Identifier templateId() {
            return TEMPLATE_ID;
        }

        @Override
        protected void buildFoundation(
                WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
            for (int x = 0; x <= WIDTH; x++) {
                for (int z = 0; z <= DEPTH; z++) {
                    this.fillFoundationColumn(
                            level, Blocks.STONE_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
                }
            }
        }
    }

    public static class NTMRuin2Piece extends NtmComponentPiece {
        private static final int WIDTH = 7, HEIGHT = 5, DEPTH = 10;

        public NTMRuin2Piece(BlockPos origin, RandomSource random) {
            this(origin, getRandomHorizontalDirection(random));
        }

        private NTMRuin2Piece(BlockPos origin, Direction direction) {
            super(
                    HbmStructureTypes.NTM_RUIN2_PIECE.get(),
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

        public NTMRuin2Piece(StructurePieceSerializationContext context, CompoundTag tag) {
            super(HbmStructureTypes.NTM_RUIN2_PIECE.get(), context, tag);
        }

        @Override
        protected void addAdditionalSaveData(
                StructurePieceSerializationContext context, CompoundTag tag) {}

        public static final Identifier TEMPLATE_ID = RegistryUtil.id("component/ruin2");

        @Override
        protected Identifier templateId() {
            return TEMPLATE_ID;
        }

        @Override
        protected void buildFoundation(
                WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
            for (int x = 0; x <= WIDTH; x++) {
                for (int z = 0; z <= DEPTH; z++) {
                    this.fillFoundationColumn(
                            level, Blocks.STONE_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
                }
            }
        }
    }

    public static class NTMRuin3Piece extends NtmComponentPiece {
        private static final int WIDTH = 8, HEIGHT = 3, DEPTH = 10;

        public NTMRuin3Piece(BlockPos origin, RandomSource random) {
            this(origin, getRandomHorizontalDirection(random));
        }

        private NTMRuin3Piece(BlockPos origin, Direction direction) {
            super(
                    HbmStructureTypes.NTM_RUIN3_PIECE.get(),
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

        public NTMRuin3Piece(StructurePieceSerializationContext context, CompoundTag tag) {
            super(HbmStructureTypes.NTM_RUIN3_PIECE.get(), context, tag);
        }

        @Override
        protected void addAdditionalSaveData(
                StructurePieceSerializationContext context, CompoundTag tag) {}

        public static final Identifier TEMPLATE_ID = RegistryUtil.id("component/ruin3");

        @Override
        protected Identifier templateId() {
            return TEMPLATE_ID;
        }

        @Override
        protected void buildFoundation(
                WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
            BlockState stonebrick = Blocks.STONE_BRICKS.defaultBlockState();
            for (int z = 0; z <= DEPTH; z++)
                this.fillFoundationColumn(level, stonebrick, 0, -1, z, chunkBB);
            for (int z = 0; z <= DEPTH; z++)
                this.fillFoundationColumn(level, stonebrick, 8, -1, z, chunkBB);
            for (int x = 1; x <= WIDTH; x++)
                this.fillFoundationColumn(level, stonebrick, x, -1, 0, chunkBB);
            for (int x = 1; x <= WIDTH; x++)
                this.fillFoundationColumn(level, stonebrick, x, -1, 4, chunkBB);
        }
    }

    public static class NTMRuin4Piece extends NtmComponentPiece {
        private static final int WIDTH = 10, HEIGHT = 2, DEPTH = 11;

        public NTMRuin4Piece(BlockPos origin, RandomSource random) {
            this(origin, getRandomHorizontalDirection(random));
        }

        private NTMRuin4Piece(BlockPos origin, Direction direction) {
            super(
                    HbmStructureTypes.NTM_RUIN4_PIECE.get(),
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

        public NTMRuin4Piece(StructurePieceSerializationContext context, CompoundTag tag) {
            super(HbmStructureTypes.NTM_RUIN4_PIECE.get(), context, tag);
        }

        @Override
        protected void addAdditionalSaveData(
                StructurePieceSerializationContext context, CompoundTag tag) {}

        public static final Identifier TEMPLATE_ID = RegistryUtil.id("component/ruin4");

        @Override
        protected Identifier templateId() {
            return TEMPLATE_ID;
        }

        @Override
        protected void buildFoundation(
                WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
            BlockState stonebrick = Blocks.STONE_BRICKS.defaultBlockState();
            for (int z = 0; z <= DEPTH; z++)
                this.fillFoundationColumn(level, stonebrick, 0, -1, z, chunkBB);
            for (int z = 5; z <= DEPTH; z++)
                this.fillFoundationColumn(level, stonebrick, 10, -1, z, chunkBB);
            for (int z = 0; z <= 4; z++)
                this.fillFoundationColumn(level, stonebrick, 5, -1, z, chunkBB);
            for (int x = 1; x <= 9; x++)
                this.fillFoundationColumn(level, stonebrick, x, -1, 11, chunkBB);
            for (int x = 1; x <= 4; x++)
                this.fillFoundationColumn(level, stonebrick, x, -1, 0, chunkBB);
            for (int x = 5; x <= 9; x++)
                this.fillFoundationColumn(level, stonebrick, x, -1, 5, chunkBB);
        }
    }
}
