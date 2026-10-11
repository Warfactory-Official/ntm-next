// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class LargeOfficePiece extends NtmComponentPiece {

    private static final int WIDTH = 14;
    private static final int HEIGHT = 5;
    private static final int DEPTH = 12;

    public LargeOfficePiece(BlockPos origin, RandomSource random) {
        this(origin, getRandomHorizontalDirection(random));
    }

    private LargeOfficePiece(BlockPos origin, Direction direction) {
        super(
                HbmStructureTypes.NTM_LARGE_OFFICE_PIECE.get(),
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

    public LargeOfficePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_LARGE_OFFICE_PIECE.get(), context, tag);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {}

    public static final Identifier TEMPLATE_ID = Library.id("component/large_office");

    @Override
    protected Identifier templateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected void buildFoundation(WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        BlockState stoneBricks = Blocks.STONE_BRICKS.defaultBlockState();
        for (int x = 5; x <= 14; x++)
            for (int z = 0; z <= 1; z++)
                this.fillFoundationColumn(level, stoneBricks, x, -1, z, chunkBB);
        for (int x = 0; x <= 14; x++)
            for (int z = 2; z <= 7; z++)
                this.fillFoundationColumn(level, stoneBricks, x, -1, z, chunkBB);
        for (int x = 0; x <= 8; x++)
            for (int z = 8; z <= 12; z++)
                this.fillFoundationColumn(level, stoneBricks, x, 0, z, chunkBB);
        for (int x = 9; x <= 14; x++)
            for (int z = 8; z <= 12; z++)
                this.fillFoundationColumn(level, stoneBricks, x, -1, z, chunkBB);
    }

    @Override
    protected void buildAfterTemplate(
            WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        if (!chunkBB.isInside(this.getWorldPos(6, 1, 1))) return;
        if (random.nextInt(2) == 0) {
            this.generateLoreBook(level, chunkBB, 6, 1, 1, 7, LoreBooks.office(random));
        }
    }
}
