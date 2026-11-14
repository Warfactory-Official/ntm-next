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

import com.hbm.blockentity.bomb.LandmineBlockEntity;
import com.hbm.blockentity.network.RadioTorchBlockEntity;
import com.hbm.blockentity.ordnance.launch.LaunchSiloBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.network.RadioTorchBlock;
import com.hbm.util.RegistryUtil;
import com.hbm.world.gen.WorldgenHeight;
import com.hbm.world.gen.component.NtmComponentPiece;
import com.hbm.world.structure.HbmStructureTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class SiloComponentPiece extends NtmComponentPiece {

    private static final int WIDTH = 42;
    private static final int HEIGHT = 29;
    private static final int DEPTH = 26;
    private final int freq;
    private final int freqHatch;

    public SiloComponentPiece(BlockPos origin, RandomSource random) {
        this(origin, getRandomHorizontalDirection(random), random.nextInt(), random.nextInt());
    }

    public static SiloComponentPiece create(Structure.GenerationContext context, BlockPos origin) {
        SiloComponentPiece piece = new SiloComponentPiece(origin, context.random());
        BlockPos first = piece.getWorldPos(13, 25, 2);
        BlockPos last = piece.getWorldPos(42, 25, 20);
        int minX = Math.min(first.getX(), last.getX()), maxX = Math.max(first.getX(), last.getX());
        int minZ = Math.min(first.getZ(), last.getZ()), maxZ = Math.max(first.getZ(), last.getZ());
        int surface = WorldgenHeight.averageGround(context, minX, minZ, maxX, maxZ);
        piece.move(0, surface - 1 - piece.getWorldY(25), 0);
        return piece;
    }

    private SiloComponentPiece(BlockPos origin, Direction direction, int freq, int freqHatch) {
        super(
                HbmStructureTypes.NTM_SILO_PIECE.get(),
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
        this.freq = freq;
        this.freqHatch = freqHatch;
    }

    public SiloComponentPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_SILO_PIECE.get(), context, tag);
        this.freq = tag.getIntOr("freq", 0);
        this.freqHatch = tag.getIntOr("freqHatch", 0);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("freq", this.freq);
        tag.putInt("freqHatch", this.freqHatch);
    }

    private void placeRadioTorch(
            WorldGenLevel level,
            BoundingBox chunkBB,
            RadioTorchBlock torch,
            int legacyMeta,
            int frequency,
            int x,
            int y,
            int z) {
        BlockPos pos = this.getWorldPos(x, y, z);
        if (!chunkBB.isInside(pos)) return;
        this.placeBlock(
                level,
                torch.defaultBlockState()
                        .setValue(RadioTorchBlock.FACING, Direction.from3DDataValue(legacyMeta))
                        .setValue(RadioTorchBlock.LIT, false),
                x,
                y,
                z,
                chunkBB);
        if (level.getBlockEntity(pos) instanceof RadioTorchBlockEntity rtty) {
            rtty.hbm$syncSet$RadioTorchBlockEntity$channel(Integer.toString(frequency));
            rtty.hbm$syncSet$RadioTorchBlockEntity$lastState(0);
            rtty.setChanged();
        }
    }

    private void fillWithMines(
            WorldGenLevel level,
            BoundingBox chunkBB,
            RandomSource random,
            BlockPos.MutableBlockPos pos,
            BlockState mine,
            int x1,
            int y1,
            int z1,
            int x2,
            int y2,
            int z2) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                int worldX = this.getWorldX(x, z);
                int worldZ = this.getWorldZ(x, z);
                if (worldX < chunkBB.minX()
                        || worldX > chunkBB.maxX()
                        || worldZ < chunkBB.minZ()
                        || worldZ > chunkBB.maxZ()) continue;
                for (int y = y1; y <= y2; y++) {
                    int worldY = this.getWorldY(y);
                    pos.set(worldX, worldY, worldZ);
                    if (random.nextInt(15) != 0 || !level.getBlockState(pos).isAir()) continue;
                    pos.setY(worldY - 1);
                    if (level.getBlockState(pos).isAir()
                            || worldY < chunkBB.minY()
                            || worldY > chunkBB.maxY()) continue;
                    pos.setY(worldY);
                    level.setBlock(pos, mine, 2);
                    if (level.getBlockEntity(pos) instanceof LandmineBlockEntity be)
                        be.deferUntilPlayerNear();
                }
            }
        }
    }

    public static final Identifier TEMPLATE_ID = RegistryUtil.id("component/silo");

    @Override
    protected Identifier templateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected void buildFoundation(WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        BlockState concreteExtMachine = hbmState("concrete_ext_machine");
        for (int x = 13; x <= 42; x++) {
            for (int z = 2; z <= 20; z++)
                this.fillFoundationColumn(level, concreteExtMachine, x, 24, z, chunkBB);
        }
    }

    @Override
    protected void buildAfterTemplate(
            WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        this.placeRadioTorch(
                level, chunkBB, ModBlocks.RADIO_TORCH_RECEIVER.get(), 1, freqHatch, 16, 25, 17);
        this.placeRadioTorch(
                level, chunkBB, ModBlocks.RADIO_TORCH_SENDER.get(), 0, freq, 26, 20, 8);
        this.placeRadioTorch(
                level, chunkBB, ModBlocks.RADIO_TORCH_SENDER.get(), 0, freqHatch, 25, 20, 7);
        this.placeRadioTorch(
                level, chunkBB, ModBlocks.RADIO_TORCH_RECEIVER.get(), 3, freq, 19, 0, 14);

        BlockPos.MutableBlockPos pos = this.getWorldPos(19, 1, 14);
        if (chunkBB.isInside(pos)
                && level.getBlockEntity(pos) instanceof LaunchSiloBlockEntity launchPad) {
            launchPad.armPayload();
        }

        BlockState mine =
                ModBlocks.MINE_AP
                        .get()
                        .defaultBlockState()
                        .rotate(legacyRotation(this.getOrientation()));
        this.fillWithMines(level, chunkBB, random, pos, mine, 2, 17, 9, 11, 17, 11);
        this.fillWithMines(level, chunkBB, random, pos, mine, 9, 17, 17, 11, 17, 24);
        this.fillWithMines(level, chunkBB, random, pos, mine, 5, 17, 23, 6, 17, 25);
        this.fillWithMines(level, chunkBB, random, pos, mine, 27, 13, 13, 33, 13, 15);
        this.fillWithMines(level, chunkBB, random, pos, mine, 1, 9, 7, 6, 9, 11);
        this.fillWithMines(level, chunkBB, random, pos, mine, 8, 9, 17, 10, 9, 22);
        this.fillWithMines(level, chunkBB, random, pos, mine, 27, 5, 13, 30, 5, 15);
    }
}
