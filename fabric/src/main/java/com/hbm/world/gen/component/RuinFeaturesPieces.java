// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.component;

import com.hbm.lib.Library;
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

        public static final Identifier TEMPLATE_ID = Library.id("component/ruin1");

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

        public static final Identifier TEMPLATE_ID = Library.id("component/ruin2");

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

        public static final Identifier TEMPLATE_ID = Library.id("component/ruin3");

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

        public static final Identifier TEMPLATE_ID = Library.id("component/ruin4");

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
