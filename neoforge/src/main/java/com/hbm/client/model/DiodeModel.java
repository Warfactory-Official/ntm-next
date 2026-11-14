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

package com.hbm.client.model;

import com.hbm.blockentity.network.CableDiodeBlockEntity;
import com.hbm.blocks.network.CableDiodeBlock;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.RegistryUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
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

public record DiodeModel() implements BlockModel<HFRWavefrontObject> {

    public static final Identifier BASE = RegistryUtil.id("block/cable_diode_base");
    private static final Identifier OBJ = RegistryUtil.id("models/blocks/cable_neo.obj");
    private static final float OFFSET_Y = 0.5F;

    private static final float WIDTH = 0.875F * 16F;
    private static final float PAD_MIN = 0.125F * 16F;
    private static final float PAD_MAX = 0.875F * 16F;

    private static final int DOWN = 1, UP = 2, NORTH = 4, SOUTH = 8, WEST = 16, EAST = 32;
    private static final String[][] PARTS_BY_MASK = BlockModel.partsByMask(DiodeModel::selectParts);

    private static String[] selectParts(int mask) {
        String[] parts = new String[Integer.bitCount(mask)];
        int i = 0;
        if ((mask & EAST) != 0) parts[i++] = "posX";
        if ((mask & WEST) != 0) parts[i++] = "negX";
        if ((mask & UP) != 0) parts[i++] = "posY";
        if ((mask & DOWN) != 0) parts[i++] = "negY";
        if ((mask & SOUTH) != 0) parts[i++] = "negZ";
        if ((mask & NORTH) != 0) parts[i++] = "posZ";
        return parts;
    }

    public static String[] partsFor(int mask) {
        return PARTS_BY_MASK[mask];
    }

    private static void addHousing(
            List<ObjUnbakedGeometry.BoxQuad> into,
            ModelBaker baker,
            Direction facing,
            Material.Baked material) {
        float minX = facing == Direction.WEST ? WIDTH : 0F;
        float minY = facing == Direction.DOWN ? WIDTH : 0F;
        float minZ = facing == Direction.NORTH ? WIDTH : 0F;
        float maxX = 16F - (facing == Direction.EAST ? WIDTH : 0F);
        float maxY = 16F - (facing == Direction.UP ? WIDTH : 0F);
        float maxZ = 16F - (facing == Direction.SOUTH ? WIDTH : 0F);
        box(into, baker, minX, minY, minZ, maxX, maxY, maxZ, material);
    }

    private static void addPad(
            List<ObjUnbakedGeometry.BoxQuad> into, ModelBaker baker, Material.Baked material) {
        box(into, baker, PAD_MIN, PAD_MIN, PAD_MIN, PAD_MAX, PAD_MAX, PAD_MAX, material);
    }

    private static void box(
            List<ObjUnbakedGeometry.BoxQuad> into,
            ModelBaker baker,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            Material.Baked material) {
        Vector3f from = new Vector3f(x0, y0, z0);
        Vector3f to = new Vector3f(x1, y1, z1);
        for (Direction dir : Direction.VALUES) {
            into.add(
                    new ObjUnbakedGeometry.BoxQuad(
                            Boxes.face(baker, from, to, dir, material),
                            ObjUnbakedGeometry.WHITE,
                            null,
                            QuadLighting.cell(0, 0, 0)));
        }
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(OBJ);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(prepared, state.getValue(CableDiodeBlock.FACING));
    }

    public record Root(HFRWavefrontObject obj, Direction facing)
            implements BlockStateModel.UnbakedRoot {

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(BASE);
        }

        @Override
        public BlockStateModel bake(BlockState blockState, ModelBaker baker) {
            ResolvedModel carrier = baker.getModel(BASE);
            TextureSlots slots = carrier.getTopTextureSlots();
            Material.Baked housing = BlockModel.slot(baker, carrier, slots, "housing");
            Material.Baked pad = BlockModel.slot(baker, carrier, slots, "pad");
            Material.Baked stub = BlockModel.slot(baker, carrier, slots, "stub");
            Material.Baked particle = carrier.resolveParticleMaterial(slots, baker);

            List<ObjUnbakedGeometry.BoxQuad> boxes = new ArrayList<>();
            addHousing(boxes, baker, facing, housing);
            addPad(boxes, baker, pad);
            BlockStateModelPart[] parts = new BlockStateModelPart[64];
            GeometryKey[] keys = new GeometryKey[64];
            int flags = 0;
            for (int mask = 0; mask < parts.length; mask++) {
                QuadCollection quads =
                        ObjUnbakedGeometry.bakeGroups(
                                List.of(
                                        new ObjUnbakedGeometry.Group(
                                                obj,
                                                partsFor(mask),
                                                stub,
                                                BlockModelRotation.IDENTITY)),
                                boxes,
                                0F,
                                OFFSET_Y,
                                0F);
                parts[mask] =
                        new SimpleModelWrapper(quads, carrier.getTopAmbientOcclusion(), particle);
                keys[mask] = new GeometryKey(facing, mask);
                flags |= parts[mask].materialFlags();
            }
            return new Baked(parts, keys, particle, flags);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return this;
        }
    }

    private record GeometryKey(Direction facing, int mask) {}

    record Baked(
            BlockStateModelPart[] parts,
            GeometryKey[] keys,
            Material.Baked particleMaterial,
            int materialFlags)
            implements BlockStateModel {
        void prepareBoundaryFaces() {
            for (int mask = 0; mask < parts.length; mask++)
                parts[mask] = BoundaryFaces.cull(parts[mask]);
        }

        @Override
        public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
            output.add(parts[0]);
        }

        private static int mask(BlockAndTintGetter level, BlockPos pos) {
            Integer mask = level.getModelData(pos).get(CableDiodeBlockEntity.SOCKETS);
            return mask == null ? 0 : mask;
        }

        public void collectParts(
                BlockAndTintGetter level,
                BlockPos pos,
                BlockState state,
                RandomSource random,
                List<BlockStateModelPart> output) {
            output.add(parts[mask(level, pos)]);
        }

        @Override
        public Object createGeometryKey(
                BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
            return keys[mask(level, pos)];
        }
    }
}
