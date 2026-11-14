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

package com.hbm.client.model;

import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.RegistryUtil;
import com.mojang.math.OctahedralGroup;
import java.util.List;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class FluoroModel implements BlockStateModel {

    public static Factory FACTORY = FluoroModel::new;

    private static final Identifier OBJ = RegistryUtil.id("models/lights/fluorescent_lamp.obj");
    private static final String[] PARTS = {"FluoroSingle", "FluoroCap", "FluoroMid"};

    private final ModelBaker baker;
    private final ResolvedModel base;
    private final TextureSlots slots;
    private final Material.Baked particle;
    private final HFRWavefrontObject obj;
    private final Direction facing;
    private final ModelPartCache cache =
            ModelPartCache.dense(PARTS.length * Direction.values().length, this::buildPart);
    private final BlockStateModelPart fallback;

    public FluoroModel(ModelBaker baker, ResolvedModel base, Direction facing) {
        this.baker = RuntimeBaker.capture(baker, base);
        this.base = base;
        this.slots = base.getTopTextureSlots();
        this.particle = base.resolveParticleMaterial(slots, baker);
        this.obj = Meshes.faceNormals(OBJ);
        this.facing = facing;
        this.fallback = buildPart(0);
    }

    private static int keyAt(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(DirectionalBlock.FACING);
        for (Direction conn : Direction.values()) {
            if (conn.getAxis() == facing.getAxis()
                    || !level.getBlockState(pos.relative(conn)).is(state.getBlock())) {
                continue;
            }
            int count =
                    level.getBlockState(pos.relative(conn.getOpposite())).is(state.getBlock())
                            ? 2
                            : 1;
            return count * Direction.values().length + conn.get3DDataValue();
        }
        return 0;
    }

    private static int rollQuarters(Direction conn, Direction axis) {
        int flipX =
                axis == Direction.DOWN || axis == Direction.NORTH || axis == Direction.WEST
                        ? -1
                        : 1;
        int addX = axis == Direction.NORTH || axis == Direction.SOUTH ? -1 : 0;
        boolean flipNS = axis == Direction.WEST;
        return switch (conn) {
            case NORTH -> flipNS ? 2 : 0;
            case SOUTH -> !flipNS ? 2 : 0;
            case EAST -> flipX + addX;
            case WEST -> -flipX + addX;
            case UP -> -1;
            case DOWN -> 1;
        };
    }

    private static OctahedralGroup roll(int quarters) {
        return switch (Math.floorMod(quarters, 4)) {
            case 1 -> OctahedralGroup.BLOCK_ROT_X_90;
            case 2 -> OctahedralGroup.BLOCK_ROT_X_180;
            case 3 -> OctahedralGroup.BLOCK_ROT_X_270;
            default -> OctahedralGroup.IDENTITY;
        };
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
        output.add(partFor(keyAt(level, pos, state)));
    }

    public Object geometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return keyAt(level, pos, state);
    }

    private BlockStateModelPart partFor(int key) {
        return cache.get(key);
    }

    private BlockStateModelPart buildPart(int key) {
        int count = key / Direction.values().length;
        OctahedralGroup rotation = SpotlightModel.rotationFor(facing);
        if (count > 0) {
            rotation =
                    rotation.compose(
                            roll(
                                    rollQuarters(
                                            Direction.from3DDataValue(
                                                    key % Direction.values().length),
                                            facing)));
        }
        Material.Baked material =
                BlockModel.slot(baker, base, slots, ObjUnbakedGeometry.SINGLE_SLOT);
        return new SimpleModelWrapper(
                ObjUnbakedGeometry.bakeGroups(
                        obj,
                        new String[] {PARTS[count]},
                        material,
                        BlockModelRotation.get(rotation),
                        SpotlightModel.OFFSET_X,
                        SpotlightModel.OFFSET_Y,
                        SpotlightModel.OFFSET_Z,
                        false,
                        false),
                base.getTopAmbientOcclusion(),
                particle);
    }

    public interface Factory {
        FluoroModel create(ModelBaker baker, ResolvedModel base, Direction facing);
    }

    public record Root(Identifier carrier, Direction facing)
            implements BlockStateModel.UnbakedRoot {

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(carrier);
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
            return FACTORY.create(baker, baker.getModel(root.carrier()), root.facing());
        }
    }
}
