// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.component;

import com.hbm.itempool.LoreBooks;
import com.hbm.lib.Library;
import com.hbm.world.structure.HbmStructureTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class LargeOfficeCornerPiece extends NtmComponentPiece {

    private static final int WIDTH = 11;
    private static final int HEIGHT = 15;
    private static final int DEPTH = 14;

    public LargeOfficeCornerPiece(BlockPos origin, RandomSource random) {
        this(origin, getRandomHorizontalDirection(random));
    }

    private LargeOfficeCornerPiece(BlockPos origin, Direction direction) {
        super(
                HbmStructureTypes.NTM_LARGE_OFFICE_CORNER_PIECE.get(),
                0,
                StructurePiece.makeBoundingBox(
                        origin.getX(),
                        origin.getY() - 1,
                        origin.getZ(),
                        direction,
                        WIDTH + 1,
                        HEIGHT + 1,
                        DEPTH + 1));
        this.setOrientation(direction);
    }

    public LargeOfficeCornerPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_LARGE_OFFICE_CORNER_PIECE.get(), context, tag);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {}

    public static final Identifier TEMPLATE_ID = Library.id("component/large_office_corner");

    @Override
    protected Identifier templateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected void buildFoundation(WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        BlockState stoneBricks = Blocks.STONE_BRICKS.defaultBlockState();
        for (int x = 4; x <= 11; x++)
            for (int z = 0; z <= 2; z++)
                this.fillFoundationColumn(level, stoneBricks, x, -1, z, chunkBB);
        for (int x = 1; x <= 11; x++)
            for (int z = 3; z <= 13; z++)
                this.fillFoundationColumn(level, stoneBricks, x, -1, z, chunkBB);
        for (int x = 0; x <= 4; x++)
            for (int z = 14; z <= 14; z++)
                this.fillFoundationColumn(level, stoneBricks, x, -1, z, chunkBB);
        for (int x = 0; x <= 0; x++)
            for (int z = 10; z <= 13; z++)
                this.fillFoundationColumn(level, stoneBricks, x, -1, z, chunkBB);
    }

    @Override
    protected void buildAfterTemplate(
            WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        if (!chunkBB.isInside(this.getWorldPos(1, 9, 13))) return;
        if (random.nextInt(2) == 0) {
            this.generateLoreBook(level, chunkBB, 1, 9, 13, 7, LoreBooks.office(random));
        }
    }
}
