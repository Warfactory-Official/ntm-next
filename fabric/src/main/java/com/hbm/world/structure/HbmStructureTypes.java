/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.world.structure;

import com.hbm.registration.Registrar;
import com.hbm.registration.RegistryHandle;
import com.hbm.world.gen.component.*;
import com.hbm.world.gen.component.*;
import com.hbm.world.gen.dungeon.JungleDungeonPiece;
import com.hbm.world.gen.dungeon.JungleDungeonStructure;
import com.hbm.world.gen.nbt.ConductorPoolElement;
import com.hbm.world.gen.nbt.LegacyPiecePoolElement;
import com.hbm.world.gen.nbt.LegacyStartPoolElement;
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
    public static RegistryHandle<StructurePieceType> JUNGLE_DUNGEON_PIECE;
    public static RegistryHandle<StructurePoolElementType<ParentOverlapPoolElement>>
            PARENT_OVERLAP_POOL_ELEMENT;
    public static RegistryHandle<StructurePoolElementType<LegacyStartPoolElement>>
            LEGACY_START_POOL_ELEMENT;
    public static RegistryHandle<StructurePoolElementType<ConductorPoolElement>>
            CONDUCTOR_POOL_ELEMENT;
    public static RegistryHandle<StructurePoolElementType<LegacyPiecePoolElement>>
            LEGACY_PIECE_POOL_ELEMENT;
    public static RegistryHandle<StructureType<LegacyJigsawStructure>> LEGACY_JIGSAW;

    public static RegistryHandle<StructureType<AncientTombStructure>> ANCIENT_TOMB;
    public static RegistryHandle<StructurePieceType> ANCIENT_TOMB_PIECE;

    public static RegistryHandle<StructureType<DesertAtomStructure>> DESERT_ATOM;
    public static RegistryHandle<StructurePieceType> DESERT_ATOM_PIECE;
    public static RegistryHandle<StructureType<SpaceshipStructure>> SPACESHIP;
    public static RegistryHandle<StructurePieceType> SPACESHIP_PIECE;

    public static RegistryHandle<StructureType<NullSpawnStructure>> NULL_SPAWN;
    public static RegistryHandle<StructurePieceType> NULL_SPAWN_PIECE;

    private HbmStructureTypes() {}

    public static void register() {
        SINGLE_PIECE = Registrar.registerStructureType("single_piece", SinglePieceStructure.CODEC);
        CONDUCTOR_POOL_ELEMENT =
                Registrar.registerStructurePoolElementType(
                        "conductor_template", ConductorPoolElement.CODEC);
        PARENT_OVERLAP_POOL_ELEMENT =
                Registrar.registerStructurePoolElementType(
                        "parent_overlap", ParentOverlapPoolElement.CODEC);
        LEGACY_START_POOL_ELEMENT =
                Registrar.registerStructurePoolElementType(
                        "legacy_start", LegacyStartPoolElement.CODEC);
        LEGACY_PIECE_POOL_ELEMENT =
                Registrar.registerStructurePoolElementType(
                        "legacy_piece", LegacyPiecePoolElement.CODEC);
        LEGACY_JIGSAW =
                Registrar.registerStructureType("legacy_jigsaw", LegacyJigsawStructure.CODEC);
        NULL_SPAWN = Registrar.registerStructureType("null_spawn", NullSpawnStructure.CODEC);
        NULL_SPAWN_PIECE =
                Registrar.registerStructurePieceType("null_spawn_piece", NullSpawnPiece::new);
        NTM_FEATURES = Registrar.registerStructureType("features", NtmFeaturesStructure.CODEC);
        NTM_PROCEDURAL =
                Registrar.registerStructureType("procedural", NtmProceduralStructure.CODEC);
        NTM_PROCEDURAL_PIECE =
                Registrar.registerStructurePieceType("procedural_piece", NtmProceduralPiece::new);
        NTM_HOUSE1_PIECE =
                Registrar.registerStructurePieceType("house1_piece", NTMHouse1Piece::new);
        NTM_HOUSE2_PIECE =
                Registrar.registerStructurePieceType("house2_piece", NTMHouse2Piece::new);
        NTM_LAB1_PIECE = Registrar.registerStructurePieceType("lab1_piece", NTMLab1Piece::new);
        NTM_LAB2_PIECE = Registrar.registerStructurePieceType("lab2_piece", NTMLab2Piece::new);
        NTM_RURAL_HOUSE1_PIECE =
                Registrar.registerStructurePieceType("rural_house1_piece", RuralHouse1Piece::new);
        NTM_LARGE_OFFICE_PIECE =
                Registrar.registerStructurePieceType("large_office_piece", LargeOfficePiece::new);
        NTM_LARGE_OFFICE_CORNER_PIECE =
                Registrar.registerStructurePieceType(
                        "large_office_corner_piece", LargeOfficeCornerPiece::new);
        NTM_SILO_PIECE =
                Registrar.registerStructurePieceType("silo_piece", SiloComponentPiece::new);
        NTM_RUIN1_PIECE =
                Registrar.registerStructurePieceType(
                        "ruin1_piece", RuinFeaturesPieces.NTMRuin1Piece::new);
        NTM_RUIN2_PIECE =
                Registrar.registerStructurePieceType(
                        "ruin2_piece", RuinFeaturesPieces.NTMRuin2Piece::new);
        NTM_RUIN3_PIECE =
                Registrar.registerStructurePieceType(
                        "ruin3_piece", RuinFeaturesPieces.NTMRuin3Piece::new);
        NTM_RUIN4_PIECE =
                Registrar.registerStructurePieceType(
                        "ruin4_piece", RuinFeaturesPieces.NTMRuin4Piece::new);
        SELLAFIELD_CRATER =
                Registrar.registerStructureType(
                        "sellafield_crater", SellafieldCraterStructure.CODEC);
        SELLAFIELD_CRATER_PIECE =
                Registrar.registerStructurePieceType(
                        "sellafield_crater_piece", SellafieldCraterPiece::new);
        OIL_BUBBLE = Registrar.registerStructureType("oil_bubble", OilBubbleStructure.CODEC);
        OIL_BUBBLE_PIECE =
                Registrar.registerStructurePieceType("oil_bubble_piece", OilBubblePiece::new);
        NTM_BUNKER = Registrar.registerStructureType("bunker", NtmBunkerStructure.CODEC);
        NTM_BUNKER_STARTING_HUB =
                Registrar.registerStructurePieceType("bunker_starting_hub", StartingHubPiece::new);
        NTM_BUNKER_CORRIDOR =
                Registrar.registerStructurePieceType("bunker_corridor", CorridorPiece::new);
        NTM_BUNKER_BEDROOM_L =
                Registrar.registerStructurePieceType("bunker_bedroom_l", BedroomLPiece::new);
        NTM_BUNKER_FUN_JUNCTION =
                Registrar.registerStructurePieceType("bunker_fun_junction", FunJunctionPiece::new);
        NTM_BUNKER_BATHROOM_L =
                Registrar.registerStructurePieceType("bunker_bathroom_l", BathroomLPiece::new);
        NTM_BUNKER_LABORATORY =
                Registrar.registerStructurePieceType("bunker_laboratory", LaboratoryPiece::new);
        NTM_BUNKER_POWER_ROOM =
                Registrar.registerStructurePieceType("bunker_power_room", PowerRoomPiece::new);
        JUNGLE_DUNGEON =
                Registrar.registerStructureType("jungle_dungeon", JungleDungeonStructure.CODEC);
        JUNGLE_DUNGEON_PIECE =
                Registrar.registerStructurePieceType(
                        "jungle_dungeon_piece", JungleDungeonPiece::new);
        ANCIENT_TOMB = Registrar.registerStructureType("ancient_tomb", AncientTombStructure.CODEC);
        ANCIENT_TOMB_PIECE =
                Registrar.registerStructurePieceType("ancient_tomb_piece", AncientTombPiece::new);
        DESERT_ATOM = Registrar.registerStructureType("desert_atom", DesertAtomStructure.CODEC);
        DESERT_ATOM_PIECE =
                Registrar.registerStructurePieceType(
                        "desert_atom_piece",
                        (context, tag) ->
                                new TemplateStructurePiece(DESERT_ATOM_PIECE.get(), context, tag));
        SPACESHIP = Registrar.registerStructureType("spaceship", SpaceshipStructure.CODEC);
        SPACESHIP_PIECE =
                Registrar.registerStructurePieceType(
                        "spaceship_piece",
                        (context, tag) ->
                                new TemplateStructurePiece(SPACESHIP_PIECE.get(), context, tag));
    }
}
