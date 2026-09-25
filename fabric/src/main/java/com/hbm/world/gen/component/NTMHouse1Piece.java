// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.component;

import com.hbm.itempool.ComponentLoot;
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
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class NTMHouse1Piece extends NtmComponentPiece {

    public static final Identifier TEMPLATE_ID = Library.id("component/house1");

    private static final int WIDTH = 9;
    private static final int HEIGHT = 4;
    private static final int DEPTH = 6;

    public NTMHouse1Piece(BlockPos origin, RandomSource random) {
        this(origin, getRandomHorizontalDirection(random));
    }

    private NTMHouse1Piece(BlockPos origin, Direction direction) {
        super(
                HbmStructureTypes.NTM_HOUSE1_PIECE.get(),
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

    public NTMHouse1Piece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_HOUSE1_PIECE.get(), context, tag);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {}

    @Override
    protected Identifier templateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected void buildFoundation(WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        for (int x = 0; x <= WIDTH; x++) {
            for (int z = 0; z <= DEPTH; z++) {
                this.fillFoundationColumn(
                        level, Blocks.SANDSTONE.defaultBlockState(), x, -1, z, chunkBB);
            }
        }
    }

    public static final PieceGeometry GEOMETRY =
            (out, variant) -> {
                List<WeightedOption> sandstone = sandstoneTable();
                BlockState sand = Blocks.SAND.defaultBlockState();
                BlockState air = Blocks.AIR.defaultBlockState();
                BlockState fence = Blocks.OAK_FENCE.defaultBlockState();
                BlockState slab = Blocks.SANDSTONE_SLAB.defaultBlockState();

                out.selectorBox(0, 0, 0, 9, 0, 0, sandstone);
                out.selectorBox(0, 1, 0, 1, 1, 0, sandstone);
                out.block(2, 1, 0, fence);
                out.selectorBox(3, 1, 0, 5, 1, 0, sandstone);
                out.block(6, 1, 0, fence);
                out.block(7, 1, 0, fence);
                out.selectorBox(8, 1, 0, 9, 1, 0, sandstone);
                out.selectorBox(0, 2, 0, 7, 2, 0, sandstone);
                out.selectorBox(0, 0, 0, 0, 1, 6, sandstone);
                out.block(0, 2, 1, slab);
                out.box(0, 2, 3, 0, 2, 6, slab, air);
                out.selectorBox(1, 0, 6, 1, 1, 6, sandstone);
                out.selectorBox(3, 0, 6, 9, 1, 6, sandstone);
                out.selectorBox(1, 2, 6, 3, 2, 6, sandstone);
                out.box(4, 2, 6, 5, 2, 6, slab, air);
                out.block(7, 2, 6, slab);
                out.selectorBox(9, 0, 0, 9, 0, 6, sandstone);
                out.maybeBox(9, 1, 1, 9, 1, 5, 0.65F, sand, air);

                out.selectorBox(4, 0, 1, 4, 1, 3, sandstone);
                out.block(4, 0, 4, hbmState("reinforced_sand"));

                out.block(1, 0, 1, hbmState("crate_weapon"));

                out.container(
                        3,
                        0,
                        1,
                        Blocks.CHEST.defaultBlockState(),
                        ComponentLoot.GENERIC_8_9_UNSCALED);
                out.box(5, 0, 1, 6, 0, 1, hbmState("crate"), air);
                out.block(7, 0, 1, sand);
                out.maybeBox(
                        8, 0, 1, 8, 0, 1, 0.25F, hbmState("crate_metal"), hbmState("crate_metal"));
                out.maybeBox(1, 0, 2, 3, 0, 5, 0.25F, sand, air);
                out.maybeBox(5, 0, 2, 8, 0, 5, 0.25F, sand, air);
            };
}
