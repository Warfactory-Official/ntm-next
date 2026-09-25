// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.structure;

import com.hbm.itempool.ComponentLoot;
import com.hbm.lib.Library;
import com.hbm.world.gen.component.NtmComponentPiece;
import com.hbm.world.gen.nbt.PieceGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class NtmProceduralPiece extends NtmComponentPiece {

    public static final Identifier TEMPLATE_ID = Library.id("component/antenna");

    private static final int FOOTPRINT = 2;
    private static final int TOP = 20;

    public NtmProceduralPiece(BlockPos origin) {
        super(
                HbmStructureTypes.NTM_PROCEDURAL_PIECE.get(),
                0,
                new BoundingBox(
                        origin.getX(),
                        origin.getY(),
                        origin.getZ(),
                        origin.getX() + FOOTPRINT,
                        origin.getY() + TOP,
                        origin.getZ() + FOOTPRINT));

        this.setOrientation(null);
    }

    public NtmProceduralPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_PROCEDURAL_PIECE.get(), context, tag);
    }

    @Override
    protected Identifier templateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {}

    private static BlockState facing(BlockState state, Direction dir) {
        return state.setValue(HorizontalDirectionalBlock.FACING, dir);
    }

    public static final PieceGeometry GEOMETRY =
            (out, variant) -> {
                BlockState decoSteel = hbmState("deco_steel");
                BlockState poleN = facing(hbmState("steel_poles"), Direction.NORTH);
                BlockState poleS = facing(hbmState("steel_poles"), Direction.SOUTH);
                BlockState poleW = facing(hbmState("steel_poles"), Direction.WEST);
                BlockState tapeE = facing(hbmState("tape_recorder"), Direction.EAST);
                BlockState air = Blocks.AIR.defaultBlockState();

                for (int dy = 0; dy <= 1; dy++) {
                    out.block(0, dy, 0, air);
                    out.block(1, dy, 0, poleN);
                    out.block(2, dy, 0, air);
                    out.block(0, dy, 1, poleW);
                    out.block(1, dy, 1, decoSteel);
                    out.block(2, dy, 1, tapeE);
                    out.block(0, dy, 2, air);
                    out.block(1, dy, 2, poleS);
                }
                out.block(2, 1, 2, air);

                out.container(
                        2,
                        0,
                        2,
                        Blocks.CHEST
                                .defaultBlockState()
                                .setValue(ChestBlock.FACING, Direction.EAST),
                        ComponentLoot.ANTENNA_8);

                out.block(0, 2, 0, air);
                out.block(1, 2, 0, decoSteel);
                out.block(2, 2, 0, air);
                out.block(0, 2, 1, decoSteel);
                out.block(1, 2, 1, decoSteel);
                out.block(2, 2, 1, decoSteel);
                out.block(0, 2, 2, air);
                out.block(1, 2, 2, decoSteel);
                out.block(2, 2, 2, air);

                BlockState receiverS = facing(hbmState("pole_satellite_receiver"), Direction.SOUTH);
                BlockState receiverN = facing(hbmState("pole_satellite_receiver"), Direction.NORTH);
                BlockState receiverW = facing(hbmState("pole_satellite_receiver"), Direction.WEST);
                BlockState poleTop = hbmState("pole_top");
                for (int dy = 3; dy <= TOP; dy++) {
                    BlockState mast =
                            switch (dy) {
                                case 13 -> receiverS;
                                case 17 -> receiverN;
                                case 18 -> receiverW;
                                case 20 -> poleTop;
                                default -> poleW;
                            };
                    for (int dz = 0; dz <= FOOTPRINT; dz++) {
                        for (int dx = 0; dx <= FOOTPRINT; dx++) {
                            out.block(dx, dy, dz, (dx == 1 && dz == 1) ? mast : air);
                        }
                    }
                }
            };
}
