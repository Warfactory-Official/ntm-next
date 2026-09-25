// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.component;

import com.hbm.lib.Library;
import com.hbm.world.gen.nbt.PieceGeometry;
import com.hbm.world.gen.nbt.WeightedOption;
import com.hbm.world.structure.HbmStructureTypes;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public final class RuinFeaturesPieces {

    private RuinFeaturesPieces() {}

    private static BlockState pillarY() {
        return NtmComponentPiece.hbmState("concrete_pillar");
    }

    private static BlockState pillarX() {
        return pillarY().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
    }

    private static BlockState pillarZ() {
        return pillarY().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
    }

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

        public static final PieceGeometry GEOMETRY =
                (out, variant) -> {
                    List<WeightedOption> bricks = concreteBricksTable();
                    BlockState pillarY = pillarY(), pillarX = pillarX(), pillarZ = pillarZ();
                    BlockState air = Blocks.AIR.defaultBlockState();
                    BlockState gravel = Blocks.GRAVEL.defaultBlockState();

                    out.box(0, 0, 0, 0, 6, 0, pillarY, air);
                    out.box(1, 3, 0, 3, 3, 0, pillarX, air);
                    out.box(4, 0, 0, 4, 5, 0, pillarY, air);
                    out.box(5, 3, 0, 7, 3, 0, pillarX, air);
                    out.box(8, 0, 0, 8, 5, 0, pillarY, air);
                    out.selectorBox(1, 0, 0, 3, 0, 0, bricks);
                    out.selectorBox(5, 0, 0, 7, 0, 0, bricks);
                    out.selectorBox(1, 1, 0, 1, 2, 0, bricks);
                    out.selectorBox(3, 1, 0, 3, 2, 0, bricks);
                    out.selectorBox(5, 1, 0, 5, 2, 0, bricks);
                    out.selectorBox(7, 1, 0, 7, 2, 0, bricks);
                    out.selectorBox(1, 4, 0, 3, 4, 0, bricks);
                    out.selectorBox(5, 4, 0, 7, 4, 0, bricks);

                    out.box(0, 3, 1, 0, 3, 9, pillarZ, air);
                    out.box(0, 0, 10, 0, 5, 10, pillarY, air);
                    out.selectorBox(0, 0, 1, 0, 0, 9, bricks);
                    out.selectorBox(0, 1, 1, 0, 2, 2, bricks);
                    out.selectorBox(0, 1, 4, 0, 2, 6, bricks);
                    out.selectorBox(0, 1, 8, 0, 2, 9, bricks);
                    out.selectorBox(0, 4, 1, 0, 4, 5, bricks);
                    out.selectorBox(0, 5, 1, 0, 5, 2, bricks);
                    out.selectorBox(0, 4, 8, 0, 4, 9, bricks);

                    out.box(1, 3, 10, 3, 3, 10, pillarX, air);
                    out.box(4, 0, 10, 4, 4, 10, pillarY, air);
                    out.box(5, 3, 10, 7, 3, 10, pillarX, air);
                    out.box(8, 0, 10, 8, 4, 10, pillarY, air);
                    out.selectorBox(1, 0, 10, 3, 0, 10, bricks);
                    out.selectorBox(5, 0, 10, 7, 0, 10, bricks);
                    out.selectorBox(1, 1, 10, 1, 2, 10, bricks);
                    out.selectorBox(3, 1, 10, 3, 2, 10, bricks);
                    out.selectorBox(5, 1, 10, 5, 2, 10, bricks);
                    out.selectorBox(7, 1, 10, 7, 2, 10, bricks);

                    out.box(8, 3, 1, 8, 3, 2, pillarZ, air);
                    out.box(8, 3, 9, 8, 3, 9, pillarZ, air);
                    out.selectorBox(8, 0, 1, 8, 0, 4, bricks);
                    out.selectorBox(8, 1, 1, 8, 2, 2, bricks);
                    out.selectorBox(8, 0, 6, 8, 0, 6, bricks);
                    out.selectorBox(8, 0, 8, 8, 1, 9, bricks);
                    out.selectorBox(8, 2, 9, 8, 2, 9, bricks);

                    out.maybeBox(1, 0, 1, 7, 0, 9, 0.25F, gravel, air);
                };
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

        public static final PieceGeometry GEOMETRY =
                (out, variant) -> {
                    List<WeightedOption> bricks = concreteBricksTable();
                    BlockState pillarY = pillarY(), pillarX = pillarX(), pillarZ = pillarZ();
                    BlockState air = Blocks.AIR.defaultBlockState();
                    BlockState gravel = Blocks.GRAVEL.defaultBlockState();

                    out.box(0, 0, 0, 0, 3, 0, pillarY, air);
                    out.box(1, 3, 0, 6, 3, 0, pillarX, air);
                    out.box(7, 0, 0, 7, 5, 0, pillarY, air);
                    out.selectorBox(1, 0, 0, 6, 0, 0, bricks);
                    out.selectorBox(1, 1, 0, 1, 2, 0, bricks);
                    out.selectorBox(3, 1, 0, 4, 2, 0, bricks);
                    out.selectorBox(6, 1, 0, 6, 2, 0, bricks);
                    out.selectorBox(3, 4, 0, 6, 4, 0, bricks);
                    out.selectorBox(6, 5, 0, 6, 5, 0, bricks);

                    out.box(0, 3, 1, 0, 3, 4, pillarZ, air);
                    out.box(0, 0, 5, 0, 0, 5, pillarY, air);
                    out.box(0, 0, 10, 0, 2, 10, pillarY, air);
                    out.selectorBox(0, 0, 1, 0, 2, 3, bricks);
                    out.selectorBox(0, 0, 7, 0, 0, 9, bricks);
                    out.selectorBox(0, 1, 9, 0, 1, 9, bricks);

                    out.box(6, 3, 10, 6, 3, 10, pillarX, air);
                    out.box(7, 0, 10, 7, 3, 10, pillarY, air);
                    out.selectorBox(1, 0, 10, 6, 0, 10, bricks);
                    out.selectorBox(1, 1, 10, 1, 2, 10, bricks);
                    out.selectorBox(6, 1, 10, 6, 2, 10, bricks);

                    out.box(7, 3, 1, 7, 3, 4, pillarZ, air);
                    out.box(7, 0, 5, 7, 4, 5, pillarY, air);
                    out.box(7, 3, 8, 7, 3, 9, pillarZ, air);
                    out.selectorBox(7, 0, 1, 7, 0, 4, bricks);
                    out.selectorBox(7, 1, 1, 7, 2, 1, bricks);
                    out.selectorBox(7, 1, 3, 7, 2, 3, bricks);
                    out.selectorBox(7, 1, 4, 7, 1, 4, bricks);
                    out.selectorBox(7, 0, 6, 7, 0, 9, bricks);
                    out.selectorBox(7, 1, 6, 7, 1, 7, bricks);
                    out.selectorBox(7, 1, 9, 7, 2, 9, bricks);

                    out.maybeBox(1, 0, 1, 6, 0, 9, 0.25F, gravel, air);
                };
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

        public static final PieceGeometry GEOMETRY =
                (out, variant) -> {
                    List<WeightedOption> bricks = concreteBricksTable();
                    BlockState pillarY = pillarY();
                    BlockState air = Blocks.AIR.defaultBlockState();
                    BlockState gravel = Blocks.GRAVEL.defaultBlockState();

                    out.box(0, 0, 0, 0, 3, 0, pillarY, air);
                    out.box(8, 0, 0, 8, 1, 0, pillarY, air);
                    out.selectorBox(1, 0, 0, 7, 0, 0, bricks);
                    out.selectorBox(1, 1, 0, 1, 1, 0, bricks);
                    out.selectorBox(4, 1, 0, 4, 1, 0, bricks);
                    out.selectorBox(7, 1, 0, 7, 1, 0, bricks);
                    out.selectorBox(1, 2, 0, 6, 2, 0, bricks);

                    out.box(0, 0, 4, 0, 1, 4, pillarY, air);
                    out.block(0, 0, 10, pillarY);
                    out.selectorBox(0, 0, 1, 0, 0, 3, bricks);
                    out.selectorBox(0, 0, 5, 0, 0, 9, bricks);
                    out.selectorBox(0, 1, 5, 0, 1, 5, bricks);
                    out.selectorBox(0, 1, 7, 0, 1, 7, bricks);

                    out.box(8, 0, 4, 8, 1, 4, pillarY, air);
                    out.box(8, 0, 10, 8, 1, 10, pillarY, air);
                    out.selectorBox(8, 0, 1, 8, 1, 3, bricks);
                    out.selectorBox(8, 0, 5, 8, 0, 6, bricks);
                    out.selectorBox(8, 0, 9, 8, 0, 9, bricks);
                    out.selectorBox(7, 0, 10, 7, 0, 10, bricks);

                    out.box(4, 0, 4, 4, 2, 4, pillarY, air);
                    out.selectorBox(3, 0, 4, 3, 1, 4, bricks);
                    out.selectorBox(5, 0, 4, 7, 1, 4, bricks);

                    out.maybeBox(1, 0, 1, 7, 0, 3, 0.05F, gravel, air);
                    out.maybeBox(1, 0, 5, 7, 0, 9, 0.05F, gravel, air);
                };
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

        public static final PieceGeometry GEOMETRY =
                (out, variant) -> {
                    List<WeightedOption> bricks = concreteBricksTable();
                    BlockState pillarY = pillarY();
                    BlockState air = Blocks.AIR.defaultBlockState();
                    BlockState gravel = Blocks.GRAVEL.defaultBlockState();

                    out.box(0, 0, 0, 0, 1, 0, pillarY, air);
                    out.box(5, 0, 0, 5, 2, 0, pillarY, air);
                    out.selectorBox(1, 0, 0, 4, 0, 0, bricks);
                    out.selectorBox(4, 1, 0, 4, 1, 0, bricks);

                    out.box(5, 0, 5, 5, 2, 5, pillarY, air);
                    out.selectorBox(5, 0, 1, 5, 0, 4, bricks);
                    out.selectorBox(5, 1, 1, 5, 1, 1, bricks);
                    out.selectorBox(5, 1, 4, 5, 1, 4, bricks);
                    out.selectorBox(5, 2, 1, 5, 2, 4, bricks);

                    out.box(10, 0, 5, 10, 1, 5, pillarY, air);
                    out.selectorBox(6, 0, 5, 9, 0, 5, bricks);
                    out.selectorBox(6, 1, 5, 6, 1, 5, bricks);
                    out.selectorBox(9, 1, 5, 9, 1, 5, bricks);

                    out.box(10, 0, 11, 10, 1, 11, pillarY, air);
                    out.selectorBox(10, 0, 6, 10, 0, 10, bricks);
                    out.selectorBox(10, 1, 6, 10, 1, 8, bricks);

                    out.box(0, 0, 11, 0, 0, 11, pillarY, air);
                    out.selectorBox(1, 0, 11, 1, 0, 11, bricks);
                    out.selectorBox(6, 0, 11, 7, 0, 11, bricks);
                    out.selectorBox(9, 0, 11, 9, 0, 11, bricks);

                    out.selectorBox(0, 0, 1, 0, 0, 10, bricks);
                    out.selectorBox(0, 1, 1, 0, 1, 1, bricks);
                    out.selectorBox(0, 1, 4, 0, 1, 7, bricks);

                    out.maybeBox(1, 0, 1, 4, 0, 5, 0.05F, gravel, air);
                    out.maybeBox(1, 0, 6, 9, 0, 10, 0.05F, gravel, air);
                };
    }
}
