// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.dungeon;

import com.hbm.lib.Library;
import com.hbm.world.structure.HbmStructureTypes;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

public class JungleDungeonStructure extends Structure {

    public static final MapCodec<JungleDungeonStructure> CODEC =
            simpleCodec(JungleDungeonStructure::new);

    public static final int ROOM_WIDTH = 5, ROOM_HEIGHT = 5;

    private static final Identifier ROOM_POOL_ID = Library.id("jungle/room");
    private static final int[] FLOOR_Y = {20, 24, 28};
    private static final int MAX_DEPTH = 20;

    public JungleDungeonStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    private static void generatePieces(
            StructurePiecesBuilder builder,
            Structure.GenerationContext context,
            Holder<StructureTemplatePool> roomPool,
            int x,
            int z) {
        RandomSource random = context.random();
        List<List<PoolElementStructurePiece>> floors = new ArrayList<>();

        for (int floorY : FLOOR_Y) {

            BlockPos anchor = new BlockPos(x, floorY + 1, z);
            Optional<Structure.GenerationStub> stub =
                    JigsawPlacement.addPieces(
                            context,
                            roomPool,
                            Optional.empty(),
                            MAX_DEPTH,
                            anchor,
                            false,
                            Optional.empty(),
                            new JigsawStructure.MaxDistance(128, 16),
                            PoolAliasLookup.EMPTY,
                            JigsawStructure.DEFAULT_DIMENSION_PADDING,
                            JigsawStructure.DEFAULT_LIQUID_SETTINGS);

            List<PoolElementStructurePiece> pieces = new ArrayList<>();
            if (stub.isPresent()) {
                for (StructurePiece piece : stub.get().getPiecesBuilder().build().pieces()) {
                    if (piece instanceof PoolElementStructurePiece poolPiece) pieces.add(poolPiece);
                }
            }
            floors.add(pieces);
        }

        for (List<PoolElementStructurePiece> floor : floors) {
            for (PoolElementStructurePiece piece : floor) builder.addPiece(piece);
        }

        for (int i = 1; i < floors.size(); i++) {
            punchHoles(builder, random, floors.get(i), floors.get(i - 1));
        }
    }

    private static void punchHoles(
            StructurePiecesBuilder builder,
            RandomSource random,
            List<PoolElementStructurePiece> upperFloor,
            List<PoolElementStructurePiece> lowerFloor) {
        for (PoolElementStructurePiece piece : upperFloor) {
            if (!isPlainVariant(piece)) continue;

            if (random.nextInt(4) != 0) continue;

            BoundingBox upperBox = piece.getBoundingBox();
            for (PoolElementStructurePiece lower : lowerFloor) {
                BoundingBox lowerBox = lower.getBoundingBox();
                if (lowerBox.minX() == upperBox.minX() && lowerBox.minZ() == upperBox.minZ()) {
                    builder.addPiece(
                            new HolePunchPiece(
                                    new BlockPos(
                                            upperBox.minX(), upperBox.minY(), upperBox.minZ())));
                    return;
                }
            }
        }
    }

    private static boolean isPlainVariant(PoolElementStructurePiece piece) {
        StructurePoolElement element = piece.getElement();
        return element instanceof SinglePoolElement single
                && single.getTemplateLocation().getPath().endsWith("_plain");
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(
            Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getBlockX(8);
        int z = chunkPos.getBlockZ(8);

        Registry<StructureTemplatePool> pools =
                context.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        Optional<? extends Holder<StructureTemplatePool>> roomPool =
                pools.get(ResourceKey.create(Registries.TEMPLATE_POOL, ROOM_POOL_ID));
        if (roomPool.isEmpty()) return Optional.empty();

        BlockPos origin = new BlockPos(x, FLOOR_Y[0], z);
        return Optional.of(
                new Structure.GenerationStub(
                        origin, builder -> generatePieces(builder, context, roomPool.get(), x, z)));
    }

    @Override
    public StructureType<?> type() {
        return HbmStructureTypes.JUNGLE_DUNGEON.get();
    }
}
