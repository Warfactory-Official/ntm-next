/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
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

package com.hbm.client.model;

import com.hbm.blockentity.network.pneumatic.PneumaticTubeBlockEntity;
import com.hbm.blocks.network.pneumatic.PneumoTubeBlock;
import com.mojang.math.Quadrant;
import java.util.List;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public class PneumoTubeModel implements BlockStateModel {

    private static final Direction[] DIRS = Direction.VALUES;

    private static final float LOWER = 5F;
    private static final float UPPER = 11F;
    private static final float C_LOWER = 4F;
    private static final float C_UPPER = 12F;

    private static final int NOZZLE_NONE = 0;
    private static final int NOZZLE_IN = 1;
    private static final int NOZZLE_OUT = 2;
    private static final int NOZZLE_AIR = 3;
    private static final int[] PLAIN = new int[] {0, 0, 0, 0, 0, 0};
    public static Factory FACTORY = PneumoTubeModel::new;
    private final ModelBaker baker;
    private final ResolvedModel base;
    private final TextureSlots slots;
    private final Material.Baked particle;
    private final BlockStateModelPart fallback;
    private static final int CACHE_PARTS = 512;
    private final ModelPartCache cache =
            ModelPartCache.bounded(
                    CACHE_PARTS,
                    key ->
                            buildPart(
                                    key & 63,
                                    (key >> 6) & 63,
                                    PneumaticTubeBlockEntity.faceFromCode((key >> 12) & 7),
                                    PneumaticTubeBlockEntity.faceFromCode((key >> 15) & 7)));

    public PneumoTubeModel(ModelBaker baker, ResolvedModel base) {
        this.baker = RuntimeBaker.capture(baker, base);
        this.base = base;
        this.slots = base.getTopTextureSlots();
        this.particle = base.resolveParticleMaterial(slots, baker);
        this.fallback = buildPart(0, 0, null, null);
    }

    private static int key(int arms, int air, @Nullable Direction in, @Nullable Direction out) {
        return arms
                | (air << 6)
                | (PneumaticTubeBlockEntity.faceCode(in) << 12)
                | (PneumaticTubeBlockEntity.faceCode(out) << 15);
    }

    private static boolean has(int mask, Direction dir) {
        return (mask & (1 << dir.ordinal())) != 0;
    }

    private static int hidden(Direction a, Direction b) {
        return (1 << a.ordinal()) | (1 << b.ordinal());
    }

    private static PneumoTubeModel create(ModelBaker baker, ResolvedModel base) {
        return FACTORY.create(baker, base);
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        output.add(fallback);
    }

    @Override
    public Material.Baked particleMaterial() {
        return particle;
    }

    @Override
    public int materialFlags() {
        return fallback.materialFlags();
    }

    public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> output) {
        if (!(state.getBlock() instanceof PneumoTubeBlock)) {
            output.add(fallback);
            return;
        }
        output.add(partFor((int) geometryKey(level, pos, state)));
    }

    public Object geometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof PneumoTubeBlock)) return 0;
        int faces = PneumoTubeBlock.faceMask(state);
        int arms = 0;
        for (Direction dir : DIRS) {
            if (has(faces, dir) && PneumoTubeBlock.canConnectTo(level, pos, dir))
                arms |= 1 << dir.ordinal();
        }
        Direction in = null;
        Direction out = null;
        int air = 0;
        if (level.getBlockEntity(pos) instanceof PneumaticTubeBlockEntity tube) {
            in = tube.intakeFace;
            out = tube.outletFace;
            air = tube.airFaces & faces;
        }
        return key(arms, air, in, out);
    }

    private BlockStateModelPart partFor(int k) {
        return cache.get(k);
    }

    private BlockStateModelPart buildPart(
            int arms, int air, @Nullable Direction in, @Nullable Direction out) {
        QuadCollection.Builder b = new QuadCollection.Builder();

        Material.Baked baseTex = BlockModel.slot(baker, base, slots, "base");
        Material.Baked straight = BlockModel.slot(baker, base, slots, "straight");

        boolean pX = has(arms, Direction.EAST), nX = has(arms, Direction.WEST);
        boolean pY = has(arms, Direction.UP), nY = has(arms, Direction.DOWN);
        boolean pZ = has(arms, Direction.SOUTH), nZ = has(arms, Direction.NORTH);
        int legacyMask =
                (pX ? 32 : 0)
                        | (nX ? 16 : 0)
                        | (pY ? 8 : 0)
                        | (nY ? 4 : 0)
                        | (pZ ? 2 : 0)
                        | (nZ ? 1 : 0);
        boolean hasConnections = in != null || out != null;

        if (legacyMask == 0b110000 && !hasConnections) {
            final int[] rot = PLAIN;
            final int hiddenMask = hidden(Direction.WEST, Direction.EAST);
            final Vector3f from = new Vector3f((float) 0, LOWER, LOWER);
            final Vector3f to = new Vector3f((float) 16, UPPER, UPPER);
            this.faces(
                    b,
                    straight,
                    hiddenMask,
                    from,
                    to,
                    new UvRotation[] {
                        new UvRotation(new CuboidFace.UVs(0.0F, 11.0F, 16.0F, 5.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(0.0F, 5.0F, 16.0F, 11.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(0.0F, 5.0F, 16.0F, 11.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(0.0F, 5.0F, 16.0F, 11.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0)
                    });

        } else if (legacyMask == 0b000011 && !hasConnections) {
            final int hiddenMask2 = hidden(Direction.NORTH, Direction.SOUTH);
            final Vector3f from2 = new Vector3f(LOWER, LOWER, (float) 0);
            final Vector3f to2 = new Vector3f(UPPER, UPPER, (float) 16);
            this.faces(
                    b,
                    straight,
                    hiddenMask2,
                    from2,
                    to2,
                    new UvRotation[] {
                        new UvRotation(new CuboidFace.UVs(0.0F, 11.0F, 16.0F, 5.0F), Quadrant.R90),
                        new UvRotation(new CuboidFace.UVs(16.0F, 11.0F, 0.0F, 5.0F), Quadrant.R90),
                        new UvRotation(new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(0.0F, 5.0F, 16.0F, 11.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(0.0F, 5.0F, 16.0F, 11.0F), Quadrant.R0)
                    });

        } else if (legacyMask == 0b001100 && !hasConnections) {
            final int hiddenMask3 = hidden(Direction.DOWN, Direction.UP);
            final Vector3f from3 = new Vector3f(LOWER, (float) 0, LOWER);
            final Vector3f to3 = new Vector3f(UPPER, (float) 16, UPPER);
            this.faces(
                    b,
                    straight,
                    hiddenMask3,
                    from3,
                    to3,
                    new UvRotation[] {
                        new UvRotation(new CuboidFace.UVs(5.0F, 11.0F, 11.0F, 5.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(0.0F, 11.0F, 16.0F, 5.0F), Quadrant.R90),
                        new UvRotation(new CuboidFace.UVs(16.0F, 11.0F, 0.0F, 5.0F), Quadrant.R90),
                        new UvRotation(new CuboidFace.UVs(16.0F, 11.0F, 0.0F, 5.0F), Quadrant.R90),
                        new UvRotation(new CuboidFace.UVs(0.0F, 11.0F, 16.0F, 5.0F), Quadrant.R90)
                    });

        } else {
            int core = 0;
            for (Direction dir : DIRS) if (has(arms, dir)) core |= 1 << dir.ordinal();
            final int[] rot4 = PLAIN;
            final Vector3f from4 = new Vector3f(LOWER, LOWER, LOWER);
            final Vector3f to4 = new Vector3f(UPPER, UPPER, UPPER);
            this.faces(
                    b,
                    baseTex,
                    core,
                    from4,
                    to4,
                    new UvRotation[] {
                        new UvRotation(new CuboidFace.UVs(5.0F, 11.0F, 11.0F, 5.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0)
                    });
            for (Direction dir : DIRS) if (has(arms, dir)) arm(b, baseTex, dir);
        }

        if (in != null) nozzle(b, in, NOZZLE_IN);
        if (out != null) nozzle(b, out, NOZZLE_OUT);
        for (Direction dir : DIRS) if (has(air, dir)) nozzle(b, dir, NOZZLE_AIR);

        return new SimpleModelWrapper(b.build(), base.getTopAmbientOcclusion(), particle);
    }

    private void nozzle(QuadCollection.Builder b, Direction dir, int type) {
        Material.Baked baseTex = BlockModel.slot(baker, base, slots, "base");
        Material.Baked head =
                BlockModel.slot(
                        baker,
                        base,
                        slots,
                        type == NOZZLE_IN ? "in" : type == NOZZLE_OUT ? "out" : "connector");
        int caps = hidden(dir, dir.getOpposite());
        switch (dir) {
            case EAST -> {
                final int[] rot = PLAIN;
                final Vector3f from = new Vector3f(UPPER, LOWER, LOWER);
                final Vector3f to = new Vector3f(C_UPPER, UPPER, UPPER);
                this.faces(
                        b,
                        baseTex,
                        caps,
                        from,
                        to,
                        new UvRotation[] {
                            new UvRotation(
                                    new CuboidFace.UVs(11.0F, 11.0F, 12.0F, 5.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(11.0F, 5.0F, 12.0F, 11.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 5.0F, 5.0F, 11.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(11.0F, 5.0F, 12.0F, 11.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0)
                        });
                final int[] rot2 = PLAIN;
                final Vector3f from2 = new Vector3f(C_UPPER, C_LOWER, C_LOWER);
                final Vector3f to2 = new Vector3f((float) 16, C_UPPER, C_UPPER);
                this.faces(
                        b,
                        head,
                        0,
                        from2,
                        to2,
                        new UvRotation[] {
                            new UvRotation(
                                    new CuboidFace.UVs(12.0F, 12.0F, 16.0F, 4.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(12.0F, 4.0F, 16.0F, 12.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(0.0F, 4.0F, 4.0F, 12.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(12.0F, 4.0F, 16.0F, 12.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 4.0F, 12.0F, 12.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 4.0F, 12.0F, 12.0F), Quadrant.R0)
                        });
            }
            case WEST -> {
                final int[] rot3 = PLAIN;
                final Vector3f from3 = new Vector3f(C_LOWER, LOWER, LOWER);
                final Vector3f to3 = new Vector3f(LOWER, UPPER, UPPER);
                this.faces(
                        b,
                        baseTex,
                        caps,
                        from3,
                        to3,
                        new UvRotation[] {
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 11.0F, 5.0F, 5.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 5.0F, 5.0F, 11.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(11.0F, 5.0F, 12.0F, 11.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 5.0F, 5.0F, 11.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0)
                        });
                final int[] rot4 = PLAIN;
                final Vector3f from4 = new Vector3f((float) 0, C_LOWER, C_LOWER);
                final Vector3f to4 = new Vector3f(C_LOWER, C_UPPER, C_UPPER);
                this.faces(
                        b,
                        head,
                        0,
                        from4,
                        to4,
                        new UvRotation[] {
                            new UvRotation(
                                    new CuboidFace.UVs(0.0F, 12.0F, 4.0F, 4.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(0.0F, 4.0F, 4.0F, 12.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(12.0F, 4.0F, 16.0F, 12.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(0.0F, 4.0F, 4.0F, 12.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 4.0F, 12.0F, 12.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 4.0F, 12.0F, 12.0F), Quadrant.R0)
                        });
            }
            case UP -> {
                final int[] rot5 = PLAIN;
                final Vector3f from5 = new Vector3f(LOWER, UPPER, LOWER);
                final Vector3f to5 = new Vector3f(UPPER, C_UPPER, UPPER);
                this.faces(
                        b,
                        baseTex,
                        caps,
                        from5,
                        to5,
                        new UvRotation[] {
                            new UvRotation(
                                    new CuboidFace.UVs(5.0F, 11.0F, 11.0F, 5.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(5.0F, 4.0F, 11.0F, 5.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(5.0F, 4.0F, 11.0F, 5.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(5.0F, 4.0F, 11.0F, 5.0F), Quadrant.R0),
                            new UvRotation(new CuboidFace.UVs(5.0F, 4.0F, 11.0F, 5.0F), Quadrant.R0)
                        });
                final int[] rot6 = PLAIN;
                final Vector3f from6 = new Vector3f(C_LOWER, C_UPPER, C_LOWER);
                final Vector3f to6 = new Vector3f(C_UPPER, (float) 16, C_UPPER);
                this.faces(
                        b,
                        head,
                        0,
                        from6,
                        to6,
                        new UvRotation[] {
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 12.0F, 12.0F, 4.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 4.0F, 12.0F, 12.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 0.0F, 12.0F, 4.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 0.0F, 12.0F, 4.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 0.0F, 12.0F, 4.0F), Quadrant.R0),
                            new UvRotation(new CuboidFace.UVs(4.0F, 0.0F, 12.0F, 4.0F), Quadrant.R0)
                        });
            }
            case DOWN -> {
                final int[] rot7 = PLAIN;
                final Vector3f from7 = new Vector3f(LOWER, C_LOWER, LOWER);
                final Vector3f to7 = new Vector3f(UPPER, LOWER, UPPER);
                this.faces(
                        b,
                        baseTex,
                        caps,
                        from7,
                        to7,
                        new UvRotation[] {
                            new UvRotation(
                                    new CuboidFace.UVs(5.0F, 11.0F, 11.0F, 5.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(5.0F, 11.0F, 11.0F, 12.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(5.0F, 11.0F, 11.0F, 12.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(5.0F, 11.0F, 11.0F, 12.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(5.0F, 11.0F, 11.0F, 12.0F), Quadrant.R0)
                        });
                final int[] rot8 = PLAIN;
                final Vector3f from8 = new Vector3f(C_LOWER, (float) 0, C_LOWER);
                final Vector3f to8 = new Vector3f(C_UPPER, C_LOWER, C_UPPER);
                this.faces(
                        b,
                        head,
                        0,
                        from8,
                        to8,
                        new UvRotation[] {
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 12.0F, 12.0F, 4.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 4.0F, 12.0F, 12.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 12.0F, 12.0F, 16.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 12.0F, 12.0F, 16.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 12.0F, 12.0F, 16.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 12.0F, 12.0F, 16.0F), Quadrant.R0)
                        });
            }
            case SOUTH -> {
                final int[] rot9 = PLAIN;
                final Vector3f from9 = new Vector3f(LOWER, LOWER, UPPER);
                final Vector3f to9 = new Vector3f(UPPER, UPPER, C_UPPER);
                this.faces(
                        b,
                        baseTex,
                        caps,
                        from9,
                        to9,
                        new UvRotation[] {
                            new UvRotation(
                                    new CuboidFace.UVs(5.0F, 12.0F, 11.0F, 11.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(5.0F, 11.0F, 11.0F, 12.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(11.0F, 5.0F, 12.0F, 11.0F), Quadrant.R0),
                            new UvRotation(new CuboidFace.UVs(4.0F, 5.0F, 5.0F, 11.0F), Quadrant.R0)
                        });
                final int[] rot10 = PLAIN;
                final Vector3f from10 = new Vector3f(C_LOWER, C_LOWER, C_UPPER);
                final Vector3f to10 = new Vector3f(C_UPPER, C_UPPER, (float) 16);
                this.faces(
                        b,
                        head,
                        0,
                        from10,
                        to10,
                        new UvRotation[] {
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 16.0F, 12.0F, 12.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 12.0F, 12.0F, 16.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 4.0F, 12.0F, 12.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 4.0F, 12.0F, 12.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(12.0F, 4.0F, 16.0F, 12.0F), Quadrant.R0),
                            new UvRotation(new CuboidFace.UVs(0.0F, 4.0F, 4.0F, 12.0F), Quadrant.R0)
                        });
            }
            case NORTH -> {
                final int[] rot11 = PLAIN;
                final Vector3f from11 = new Vector3f(LOWER, LOWER, C_LOWER);
                final Vector3f to11 = new Vector3f(UPPER, UPPER, LOWER);
                this.faces(
                        b,
                        baseTex,
                        caps,
                        from11,
                        to11,
                        new UvRotation[] {
                            new UvRotation(
                                    new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 4.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(5.0F, 4.0F, 11.0F, 5.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 5.0F, 5.0F, 11.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(11.0F, 5.0F, 12.0F, 11.0F), Quadrant.R0)
                        });
                final int[] rot12 = PLAIN;
                final Vector3f from12 = new Vector3f(C_LOWER, C_LOWER, (float) 0);
                final Vector3f to12 = new Vector3f(C_UPPER, C_UPPER, C_LOWER);
                this.faces(
                        b,
                        head,
                        0,
                        from12,
                        to12,
                        new UvRotation[] {
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 4.0F, 12.0F, 0.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 0.0F, 12.0F, 4.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 4.0F, 12.0F, 12.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 4.0F, 12.0F, 12.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(0.0F, 4.0F, 4.0F, 12.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(12.0F, 4.0F, 16.0F, 12.0F), Quadrant.R0)
                        });
            }
        }
    }

    private void arm(QuadCollection.Builder b, Material.Baked tex, Direction dir) {
        int caps = hidden(dir, dir.getOpposite());
        switch (dir) {
            case EAST -> {
                final int[] rot = PLAIN;
                {
                    final Vector3f from = new Vector3f(UPPER, LOWER, LOWER);
                    final Vector3f to = new Vector3f((float) 16, UPPER, UPPER);
                    this.faces(
                            b,
                            tex,
                            caps,
                            from,
                            to,
                            new UvRotation[] {
                                new UvRotation(
                                        new CuboidFace.UVs(11.0F, 11.0F, 16.0F, 5.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(11.0F, 5.0F, 16.0F, 11.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(0.0F, 5.0F, 5.0F, 11.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(11.0F, 5.0F, 16.0F, 11.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0)
                            });
                }
            }
            case WEST -> {
                final int[] rot2 = PLAIN;
                {
                    final Vector3f from2 = new Vector3f((float) 0, LOWER, LOWER);
                    final Vector3f to2 = new Vector3f(LOWER, UPPER, UPPER);
                    this.faces(
                            b,
                            tex,
                            caps,
                            from2,
                            to2,
                            new UvRotation[] {
                                new UvRotation(
                                        new CuboidFace.UVs(0.0F, 11.0F, 5.0F, 5.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(0.0F, 5.0F, 5.0F, 11.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(11.0F, 5.0F, 16.0F, 11.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(0.0F, 5.0F, 5.0F, 11.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0)
                            });
                }
            }
            case UP -> {
                final int[] rot3 = PLAIN;
                {
                    final Vector3f from3 = new Vector3f(LOWER, UPPER, LOWER);
                    final Vector3f to3 = new Vector3f(UPPER, (float) 16, UPPER);
                    this.faces(
                            b,
                            tex,
                            caps,
                            from3,
                            to3,
                            new UvRotation[] {
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 11.0F, 11.0F, 5.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 0.0F, 11.0F, 5.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 0.0F, 11.0F, 5.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 0.0F, 11.0F, 5.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 0.0F, 11.0F, 5.0F), Quadrant.R0)
                            });
                }
            }
            case DOWN -> {
                final int[] rot4 = PLAIN;
                {
                    final Vector3f from4 = new Vector3f(LOWER, (float) 0, LOWER);
                    final Vector3f to4 = new Vector3f(UPPER, LOWER, UPPER);
                    this.faces(
                            b,
                            tex,
                            caps,
                            from4,
                            to4,
                            new UvRotation[] {
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 11.0F, 11.0F, 5.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 11.0F, 11.0F, 16.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 11.0F, 11.0F, 16.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 11.0F, 11.0F, 16.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 11.0F, 11.0F, 16.0F), Quadrant.R0)
                            });
                }
            }
            case SOUTH -> {
                final int[] rot5 = PLAIN;
                {
                    final Vector3f from5 = new Vector3f(LOWER, LOWER, UPPER);
                    final Vector3f to5 = new Vector3f(UPPER, UPPER, (float) 16);
                    this.faces(
                            b,
                            tex,
                            caps,
                            from5,
                            to5,
                            new UvRotation[] {
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 16.0F, 11.0F, 11.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 11.0F, 11.0F, 16.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(11.0F, 5.0F, 16.0F, 11.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(0.0F, 5.0F, 5.0F, 11.0F), Quadrant.R0)
                            });
                }
            }
            case NORTH -> {
                final int[] rot6 = PLAIN;
                {
                    final Vector3f from6 = new Vector3f(LOWER, LOWER, (float) 0);
                    final Vector3f to6 = new Vector3f(UPPER, UPPER, LOWER);
                    this.faces(
                            b,
                            tex,
                            caps,
                            from6,
                            to6,
                            new UvRotation[] {
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 0.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 0.0F, 11.0F, 5.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(5.0F, 5.0F, 11.0F, 11.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(0.0F, 5.0F, 5.0F, 11.0F), Quadrant.R0),
                                new UvRotation(
                                        new CuboidFace.UVs(11.0F, 5.0F, 16.0F, 11.0F), Quadrant.R0)
                            });
                }
            }
        }
    }

    private void faces(
            QuadCollection.Builder b,
            Material.Baked material,
            int hiddenMask,
            Vector3f from,
            Vector3f to,
            UvRotation[] uvs) {
        for (Direction dir : DIRS) {
            if ((hiddenMask & (1 << dir.ordinal())) != 0) continue;
            UvRotation uv = uvs[dir.ordinal()];
            b.addUnculledFace(
                    Boxes.face(
                            baker,
                            from,
                            to,
                            dir,
                            material,
                            uv.uvs(),
                            uv.quadrant(),
                            CuboidFace.NO_TINT,
                            Boxes.IDENTITY));
        }
    }

    public interface Factory {
        PneumoTubeModel create(ModelBaker baker, ResolvedModel base);
    }

    public record Family() implements SimpleBlockModel {

        @Override
        public BlockStateModel.UnbakedRoot root(Block block, BlockState state) {
            return new Root(BlockModel.base(block));
        }
    }

    public record Root(Identifier baseModel) implements BlockStateModel.UnbakedRoot {

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(baseModel);
        }

        @Override
        public BlockStateModel bake(BlockState blockState, ModelBaker baker) {
            return baker.compute(new SharedKey(this));
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return this;
        }
    }

    private record SharedKey(Root root) implements ModelBaker.SharedOperationKey<BlockStateModel> {

        @Override
        public BlockStateModel compute(ModelBaker baker) {
            return create(baker, baker.getModel(root.baseModel()));
        }
    }
}
