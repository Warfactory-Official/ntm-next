// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.structure;

import com.hbm.registration.IRegistrar;
import com.hbm.registration.RegistryHandle;
import com.hbm.world.gen.component.*;
import com.hbm.world.gen.dungeon.HolePunchPiece;
import com.hbm.world.gen.dungeon.JungleDungeonStructure;
import com.hbm.world.gen.nbt.ParentOverlapPoolElement;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;

public final class HbmStructureTypes {

    public static RegistryHandle<StructureType<SinglePieceStructure>> SINGLE_PIECE;

    public static RegistryHandle<StructureType<NtmProceduralStructure>> NTM_PROCEDURAL;

    public static RegistryHandle<StructurePieceType> NTM_PROCEDURAL_PIECE;

    public static RegistryHandle<StructureType<NtmFeaturesStructure>> NTM_FEATURES;

    public static RegistryHandle<StructurePieceType> NTM_HOUSE1_PIECE;
    public static RegistryHandle<StructurePieceType> NTM_HOUSE2_PIECE;
    public static RegistryHandle<StructurePieceType> NTM_LAB1_PIECE;
    public static RegistryHandle<StructurePieceType> NTM_LAB2_PIECE;
    public static RegistryHandle<StructurePieceType> NTM_RURAL_HOUSE1_PIECE;
    public static RegistryHandle<StructurePieceType> NTM_LARGE_OFFICE_PIECE;
    public static RegistryHandle<StructurePieceType> NTM_LARGE_OFFICE_CORNER_PIECE;
    public static RegistryHandle<StructurePieceType> NTM_SILO_PIECE;

    public static RegistryHandle<StructurePieceType> NTM_RUIN1_PIECE;
    public static RegistryHandle<StructurePieceType> NTM_RUIN2_PIECE;
    public static RegistryHandle<StructurePieceType> NTM_RUIN3_PIECE;
    public static RegistryHandle<StructurePieceType> NTM_RUIN4_PIECE;

    public static RegistryHandle<StructureType<SellafieldCraterStructure>> SELLAFIELD_CRATER;
    public static RegistryHandle<StructureType<OilBubbleStructure>> OIL_BUBBLE;

    public static RegistryHandle<StructurePieceType> SELLAFIELD_CRATER_PIECE;
    public static RegistryHandle<StructurePieceType> OIL_BUBBLE_PIECE;

    public static RegistryHandle<StructureType<NtmBunkerStructure>> NTM_BUNKER;

    public static RegistryHandle<StructurePieceType> NTM_BUNKER_STARTING_HUB;
    public static RegistryHandle<StructurePieceType> NTM_BUNKER_CORRIDOR;
    public static RegistryHandle<StructurePieceType> NTM_BUNKER_BEDROOM_L;
    public static RegistryHandle<StructurePieceType> NTM_BUNKER_FUN_JUNCTION;
    public static RegistryHandle<StructurePieceType> NTM_BUNKER_BATHROOM_L;
    public static RegistryHandle<StructurePieceType> NTM_BUNKER_LABORATORY;
    public static RegistryHandle<StructurePieceType> NTM_BUNKER_POWER_ROOM;

    public static RegistryHandle<StructureType<JungleDungeonStructure>> JUNGLE_DUNGEON;
    public static RegistryHandle<StructurePieceType> JUNGLE_HOLE_PUNCH;
    public static RegistryHandle<StructurePoolElementType<ParentOverlapPoolElement>>
            PARENT_OVERLAP_POOL_ELEMENT;

    public static RegistryHandle<StructureType<AncientTombStructure>> ANCIENT_TOMB;
    public static RegistryHandle<StructurePieceType> ANCIENT_TOMB_PIECE;

    public static RegistryHandle<StructureType<DesertAtomStructure>> DESERT_ATOM;
    public static RegistryHandle<StructurePieceType> DESERT_ATOM_PIECE;
    public static RegistryHandle<StructureType<SpaceshipStructure>> SPACESHIP;
    public static RegistryHandle<StructurePieceType> SPACESHIP_PIECE;

    public static RegistryHandle<StructureType<NullSpawnStructure>> NULL_SPAWN;
    public static RegistryHandle<StructurePieceType> NULL_SPAWN_PIECE;

    private HbmStructureTypes() {}

    public static void register(IRegistrar r) {
        SINGLE_PIECE = r.registerStructureType("single_piece", SinglePieceStructure.CODEC);
        PARENT_OVERLAP_POOL_ELEMENT =
                r.registerStructurePoolElementType(
                        "parent_overlap", ParentOverlapPoolElement.CODEC);
        NULL_SPAWN = r.registerStructureType("null_spawn", NullSpawnStructure.CODEC);
        NULL_SPAWN_PIECE = r.registerStructurePieceType("null_spawn_piece", NullSpawnPiece::new);
        NTM_FEATURES = r.registerStructureType("features", NtmFeaturesStructure.CODEC);
        NTM_PROCEDURAL = r.registerStructureType("procedural", NtmProceduralStructure.CODEC);
        NTM_PROCEDURAL_PIECE =
                r.registerStructurePieceType("procedural_piece", NtmProceduralPiece::new);
        NTM_HOUSE1_PIECE = r.registerStructurePieceType("house1_piece", NTMHouse1Piece::new);
        NTM_HOUSE2_PIECE = r.registerStructurePieceType("house2_piece", NTMHouse2Piece::new);
        NTM_LAB1_PIECE = r.registerStructurePieceType("lab1_piece", NTMLab1Piece::new);
        NTM_LAB2_PIECE = r.registerStructurePieceType("lab2_piece", NTMLab2Piece::new);
        NTM_RURAL_HOUSE1_PIECE =
                r.registerStructurePieceType("rural_house1_piece", RuralHouse1Piece::new);
        NTM_LARGE_OFFICE_PIECE =
                r.registerStructurePieceType("large_office_piece", LargeOfficePiece::new);
        NTM_LARGE_OFFICE_CORNER_PIECE =
                r.registerStructurePieceType(
                        "large_office_corner_piece", LargeOfficeCornerPiece::new);
        NTM_SILO_PIECE = r.registerStructurePieceType("silo_piece", SiloComponentPiece::new);
        NTM_RUIN1_PIECE =
                r.registerStructurePieceType("ruin1_piece", RuinFeaturesPieces.NTMRuin1Piece::new);
        NTM_RUIN2_PIECE =
                r.registerStructurePieceType("ruin2_piece", RuinFeaturesPieces.NTMRuin2Piece::new);
        NTM_RUIN3_PIECE =
                r.registerStructurePieceType("ruin3_piece", RuinFeaturesPieces.NTMRuin3Piece::new);
        NTM_RUIN4_PIECE =
                r.registerStructurePieceType("ruin4_piece", RuinFeaturesPieces.NTMRuin4Piece::new);
        SELLAFIELD_CRATER =
                r.registerStructureType("sellafield_crater", SellafieldCraterStructure.CODEC);
        SELLAFIELD_CRATER_PIECE =
                r.registerStructurePieceType("sellafield_crater_piece", SellafieldCraterPiece::new);
        OIL_BUBBLE = r.registerStructureType("oil_bubble", OilBubbleStructure.CODEC);
        OIL_BUBBLE_PIECE = r.registerStructurePieceType("oil_bubble_piece", OilBubblePiece::new);
        NTM_BUNKER = r.registerStructureType("bunker", NtmBunkerStructure.CODEC);
        NTM_BUNKER_STARTING_HUB =
                r.registerStructurePieceType("bunker_starting_hub", StartingHubPiece::new);
        NTM_BUNKER_CORRIDOR = r.registerStructurePieceType("bunker_corridor", CorridorPiece::new);
        NTM_BUNKER_BEDROOM_L = r.registerStructurePieceType("bunker_bedroom_l", BedroomLPiece::new);
        NTM_BUNKER_FUN_JUNCTION =
                r.registerStructurePieceType("bunker_fun_junction", FunJunctionPiece::new);
        NTM_BUNKER_BATHROOM_L =
                r.registerStructurePieceType("bunker_bathroom_l", BathroomLPiece::new);
        NTM_BUNKER_LABORATORY =
                r.registerStructurePieceType("bunker_laboratory", LaboratoryPiece::new);
        NTM_BUNKER_POWER_ROOM =
                r.registerStructurePieceType("bunker_power_room", PowerRoomPiece::new);
        JUNGLE_DUNGEON = r.registerStructureType("jungle_dungeon", JungleDungeonStructure.CODEC);
        JUNGLE_HOLE_PUNCH = r.registerStructurePieceType("jungle_hole_punch", HolePunchPiece::new);
        ANCIENT_TOMB = r.registerStructureType("ancient_tomb", AncientTombStructure.CODEC);
        ANCIENT_TOMB_PIECE =
                r.registerStructurePieceType("ancient_tomb_piece", AncientTombPiece::new);
        DESERT_ATOM = r.registerStructureType("desert_atom", DesertAtomStructure.CODEC);
        DESERT_ATOM_PIECE =
                r.registerStructurePieceType(
                        "desert_atom_piece",
                        (context, tag) ->
                                new TemplateStructurePiece(DESERT_ATOM_PIECE.get(), context, tag));
        SPACESHIP = r.registerStructureType("spaceship", SpaceshipStructure.CODEC);
        SPACESHIP_PIECE =
                r.registerStructurePieceType(
                        "spaceship_piece",
                        (context, tag) ->
                                new TemplateStructurePiece(SPACESHIP_PIECE.get(), context, tag));
    }
}
